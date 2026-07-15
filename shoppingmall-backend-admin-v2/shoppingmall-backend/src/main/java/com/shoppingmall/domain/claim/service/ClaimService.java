package com.shoppingmall.domain.claim.service;

import com.shoppingmall.domain.claim.dto.request.ClaimCreateRequest;
import com.shoppingmall.domain.claim.dto.response.ClaimListResponse;
import com.shoppingmall.domain.claim.dto.response.ClaimResponse;
import com.shoppingmall.domain.claim.entity.Claim;
import com.shoppingmall.domain.claim.entity.ClaimStatus;
import com.shoppingmall.domain.claim.repository.ClaimRepository;
import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.entity.OrderDetail;
import com.shoppingmall.domain.order.repository.OrderDetailRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 일반 사용자 교환·환불 클레임 서비스
 *
 * 담당 기능
 * 1. 클레임 신청
 * 2. 사용자의 클레임 목록 조회
 * 3. 클레임 단건 조회
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final OrderDetailRepository orderDetailRepository;

    /**
     * 교환 또는 환불 신청
     *
     * 처리 순서
     * 1. 사용자 소유 주문 상세 조회
     * 2. 클레임 신청 가능 배송 상태 확인
     * 3. 진행 중인 중복 클레임 확인
     * 4. 환불 예상 금액 계산
     * 5. Claim Entity 생성
     * 6. DB 저장
     * 7. 응답 DTO 반환
     */
    @Transactional
    public ClaimResponse createClaim(
            Long userId,
            ClaimCreateRequest request
    ) {
        // 사용자 본인의 주문 상세인지 함께 확인한다.
        OrderDetail orderDetail = orderDetailRepository
                .findByIdAndOrder_User_Id(
                        request.orderDetailId(),
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.ORDER_NOT_FOUND
                        )
                );

        // 현재 배송 상태에서 교환·환불 신청이 가능한지 확인한다.
        validateClaimableStatus(orderDetail);

        // 처리 중인 클레임이 이미 존재하는지 확인한다.
        validateDuplicateClaim(orderDetail.getId());

        // 현재 구조에서는 주문 상세 총액을 클레임 금액으로 사용한다.
        Integer claimAmount = orderDetail.getTotalPrice();

        Claim claim = Claim.builder()
                .orderDetail(orderDetail)
                .type(request.type())
                .reason(request.reason())
                .claimAmount(claimAmount)
                .build();

        Claim savedClaim = claimRepository.save(claim);

        return ClaimResponse.from(savedClaim);
    }

    /**
     * 사용자가 신청한 클레임 목록 조회
     */
    public ClaimListResponse getMyClaims(
            Long userId,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize =
                Math.min(Math.max(size, 1), 100);

        PageRequest pageable = PageRequest.of(
                normalizedPage,
                normalizedSize
        );

        Page<Claim> claimPage = claimRepository
                .findAllByOrderDetail_Order_User_IdOrderByCreatedAtDesc(
                        userId,
                        pageable
                );

        return ClaimListResponse.from(claimPage);
    }

    /**
     * 사용자의 클레임 단건 조회
     */
    public ClaimResponse getMyClaim(
            Long userId,
            Long claimId
    ) {
        Claim claim = claimRepository
                .findByIdAndOrderDetail_Order_User_Id(
                        claimId,
                        userId
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.CLAIM_NOT_FOUND
                        )
                );

        return ClaimResponse.from(claim);
    }

    /**
     * 구매 확정 또는 이미 취소·환불된 주문은
     * 교환·환불을 신청할 수 없도록 한다.
     */
    private void validateClaimableStatus(
            OrderDetail orderDetail
    ) {
        DeliveryStatus status =
                orderDetail.getDeliveryStatus();

        boolean claimable =
                status == DeliveryStatus.PAYMENT_COMPLETED
                || status == DeliveryStatus.PREPARING
                || status == DeliveryStatus.SHIPPING
                || status == DeliveryStatus.DELIVERED;

        if (!claimable) {
            throw new CustomException(
                    ErrorCode.INVALID_CLAIM_STATUS
            );
        }
    }

    /**
     * 동일 주문 상세에 처리 중인 클레임이 있는지 확인한다.
     */
    private void validateDuplicateClaim(
            Long orderDetailId
    ) {
        List<ClaimStatus> activeStatuses = List.of(
                ClaimStatus.REQUESTED,
                ClaimStatus.ACCEPTED,
                ClaimStatus.PROCESSING
        );

        boolean exists =
                claimRepository.existsByOrderDetail_IdAndStatusIn(
                        orderDetailId,
                        activeStatuses
                );

        if (exists) {
            throw new CustomException(
                    ErrorCode.CLAIM_ALREADY_EXISTS
            );
        }
    }
}