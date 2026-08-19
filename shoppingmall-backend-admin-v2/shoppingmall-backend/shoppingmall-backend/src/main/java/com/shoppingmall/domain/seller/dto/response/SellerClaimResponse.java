package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.claim.entity.Claim;

import java.time.LocalDateTime;

public record SellerClaimResponse(

        Long claimId,
        Long orderDetailId,
        Long productId,
        String productName,
        String buyerUsername,
        String claimType,
        String reason,
        String processReason,
        String status,
        Integer claimAmount,
        LocalDateTime orderedAt,
        LocalDateTime requestedAt,
        LocalDateTime processedAt

) {

    /**
     * Claim 엔티티를 판매자용 응답 DTO로 변환한다.
     *
     * buyerUsername(요청 고객의 로그인 아이디)과 orderedAt(구매 시점)은
     * 판매자가 클레임을 처리할 때 "누가, 언제 산 건인지" 식별할 수 있도록 함께 내려준다.
     * (SellerClaimService 가 readOnly 트랜잭션 안에서 변환하므로 지연로딩 접근이 안전하다.)
     */
    public static SellerClaimResponse from(Claim claim) {
        return new SellerClaimResponse(
                claim.getId(),
                claim.getOrderDetail().getId(),
                claim.getOrderDetail().getProduct().getId(),
                claim.getOrderDetail().getProductName(),
                claim.getOrderDetail().getOrder().getUser().getUsername(),
                claim.getType().name(),
                claim.getReason(),
                claim.getProcessReason(),
                claim.getStatus().name(),
                claim.getClaimAmount(),
                claim.getOrderDetail().getOrder().getCreatedAt(),
                claim.getCreatedAt(),
                claim.getProcessedAt()
        );
    }
}