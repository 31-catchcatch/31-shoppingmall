package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.claim.entity.Claim;
import com.shoppingmall.domain.claim.entity.ClaimStatus;
import com.shoppingmall.domain.claim.entity.ClaimType;
import com.shoppingmall.domain.claim.repository.ClaimRepository;
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

        /*
         * 5. 실제 PG 결제 취소 및 포인트 회수
         *
         * PaymentService와 PointService가 완성되면
         * 아래 위치에 실제 연동 로직을 추가한다.
         *
         * 예시:
         *
         * PaymentCancelResult paymentResult =
         *         paymentService.cancelPayment(
         *                 claim.getOrderDetail().getOrder().getId(),
         *                 claim.getClaimAmount(),
         *                 request.memo()
         *         );
         *
         * pointService.recoverUsedPoints(
         *         claim.getOrderDetail().getOrder().getUser().getId(),
         *         claim.getOrderDetail().getOrder().getUsedPointAmount()
         * );
         */

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