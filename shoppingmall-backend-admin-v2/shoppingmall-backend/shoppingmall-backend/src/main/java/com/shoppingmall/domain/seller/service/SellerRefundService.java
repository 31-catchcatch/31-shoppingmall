package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.claim.entity.Claim;
import com.shoppingmall.domain.claim.entity.ClaimStatus;
import com.shoppingmall.domain.claim.entity.ClaimType;
import com.shoppingmall.domain.claim.repository.ClaimRepository;
import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderDetail;
import com.shoppingmall.domain.point.service.PointService;
import com.shoppingmall.domain.product.repository.ProductOptionRepository;
import com.shoppingmall.domain.seller.dto.request.SellerRefundCreateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerRefundResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 판매자 최종 환불 처리 서비스
 *
 * 담당 API
 * POST /api/v1/seller/refunds
 *
 * 현재 단계에서는:
 * 1. 판매자 권한 확인
 * 2. 클레임 조회 및 소유권 확인
 * 3. 환불 가능한 클레임인지 검증
 * 4. 클레임을 완료 상태로 변경
 *
 * 추후 Payment 및 Point 도메인이 확정되면
 * PG 결제 취소와 포인트 회수 로직을 연결한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerRefundService {

    private final ClaimRepository claimRepository;
    private final SellerApplicationRepository sellerApplicationRepository;
    private final PointService pointService;
    private final ProductOptionRepository productOptionRepository;

    /**
     * 판매자가 최종 환불을 완료 처리한다.
     *
     * 처리 순서
     * 1. 승인된 판매자 확인
     * 2. 환불 대상 클레임 조회
     * 3. 해당 판매자의 상품인지 확인
     * 4. 환불 유형과 클레임 상태 검증
     * 5. PG 결제 취소 및 포인트 회수
     * 6. 클레임 완료 상태 변경
     * 7. 응답 DTO 반환
     */
    @Transactional
    public SellerRefundResponse processRefund(
            Long userId,
            SellerRefundCreateRequest request
    ) {
        // 1. 로그인 사용자가 승인된 판매자인지 확인한다.
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        // 2. 환불 대상 클레임을 조회한다.
        Claim claim = claimRepository
                .findById(request.claimId())
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.CLAIM_NOT_FOUND
                        )
                );

        // 3. 해당 판매자의 상품에 대한 클레임인지 확인한다.
        validateClaimOwnership(claim, seller);

        // 4. 최종 환불 처리가 가능한 클레임인지 확인한다.
        validateRefundableClaim(claim);

        // 5. 해당 주문상품을 환불 완료 상태로 전환한다.
        OrderDetail orderDetail = claim.getOrderDetail();
        orderDetail.markRefunded();

        // 5-0. 반품된 수량만큼 재고를 복원한다 (옵션 있는 주문만).
        //      부분 환불이어도 실제 반품된 상품이므로 이 주문상품 단위로 되돌린다.
        //      (포인트/쿠폰은 주문 전체 단위라 아래 전체환불 조건에서만 복원하지만,
        //       재고는 주문상품 단위 수량이라 여기서 항상 복원한다.)
        if (orderDetail.getProductOption() != null) {
            productOptionRepository.restoreStock(
                    orderDetail.getProductOption().getId(),
                    orderDetail.getQuantity()
            );
        }

        /*
         * 5-1. 주문에 포함된 모든 주문상품이 환불 완료되었다면(=주문 전체 환불)
         *      주문 상태를 환불로 전환하고, 사용했던 포인트/쿠폰을 되돌려 준다.
         *
         *      부분 환불(주문의 일부 상품만 환불)인 경우에는 포인트·쿠폰이
         *      주문 전체 단위로 적용되어 있어 되돌리지 않는다.
         *
         * NOTE: PG 결제 취소는 별도 PG 연동이 없는 가상결제 구조라 대상에서 제외한다.
         */
        Order order = orderDetail.getOrder();
        if (order.isFullyRefunded()) {
            order.markRefunded();

            if (order.getUsedPointAmount() > 0) {
                pointService.adjustPoint(
                        order.getUser().getId(),
                        order.getUsedPointAmount(),
                        "환불로 인한 포인트 복원 (주문번호: " + order.getOrderNumber() + ")"
                );
            }

            if (order.getUsedCoupon() != null) {
                order.getUsedCoupon().restore();
            }
        }

        // 6. 환불 처리가 끝났으므로 클레임을 완료 상태로 변경한다.
        claim.complete();

        // 환불 처리 완료 시각
        LocalDateTime refundedAt = LocalDateTime.now();

        // 7. 처리 결과를 응답 DTO로 반환한다.
        return new SellerRefundResponse(
                claim.getId(),
                claim.getOrderDetail().getId(),
                claim.getClaimAmount(),
                claim.getStatus().name(),
                request.memo(),
                refundedAt
        );
    }

    /**
     * 사용자의 가장 최근 입점 신청이 승인 상태인지 확인한다.
     */
    private SellerApplication getApprovedSellerApplication(
            Long userId
    ) {
        SellerApplication application =
                sellerApplicationRepository
                        .findFirstByUser_IdOrderByCreatedAtDesc(userId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SELLER_NOT_APPROVED
                                )
                        );

        if (application.getStatus()
                != SellerApplicationStatus.APPROVED) {
            throw new CustomException(
                    ErrorCode.SELLER_NOT_APPROVED
            );
        }

        return application;
    }

    /**
     * 환불 클레임이 현재 판매자의 상품에 대한 요청인지 확인한다.
     */
    private void validateClaimOwnership(
            Claim claim,
            SellerApplication seller
    ) {
        Long claimSellerId = claim
                .getOrderDetail()
                .getProduct()
                .getSeller()
                .getId();

        if (!claimSellerId.equals(seller.getId())) {
            throw new CustomException(
                    ErrorCode.ACCESS_DENIED
            );
        }
    }

    /**
     * 최종 환불 처리가 가능한 클레임인지 검증한다.
     *
     * RETURN 유형이며 판매자가 이미 접수하거나
     * 처리 중인 클레임만 최종 환불할 수 있다.
     */
    private void validateRefundableClaim(Claim claim) {

        // 교환 요청은 환불 완료 API로 처리할 수 없다.
        if (claim.getType() != ClaimType.RETURN) {
            throw new CustomException(
                    ErrorCode.REFUND_NOT_ALLOWED
            );
        }

        ClaimStatus status = claim.getStatus();

        boolean refundable =
                status == ClaimStatus.ACCEPTED
                        || status == ClaimStatus.PROCESSING;

        if (!refundable) {
            throw new CustomException(
                    ErrorCode.REFUND_NOT_ALLOWED
            );
        }
    }
}