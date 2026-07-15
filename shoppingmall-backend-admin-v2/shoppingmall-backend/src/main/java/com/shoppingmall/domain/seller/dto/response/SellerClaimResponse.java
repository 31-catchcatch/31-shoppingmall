package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.claim.entity.Claim;

import java.time.LocalDateTime;

public record SellerClaimResponse(

        Long claimId,
        Long orderDetailId,
        Long productId,
        String productName,
        String claimType,
        String reason,
        String processReason,
        String status,
        Integer claimAmount,
        LocalDateTime requestedAt,
        LocalDateTime processedAt

) {

    /**
     * Claim 엔티티를 판매자용 응답 DTO로 변환한다.
     */
    public static SellerClaimResponse from(Claim claim) {
        return new SellerClaimResponse(
                claim.getId(),
                claim.getOrderDetail().getId(),
                claim.getOrderDetail().getProduct().getId(),
                claim.getOrderDetail().getProductName(),
                claim.getType().name(),
                claim.getReason(),
                claim.getProcessReason(),
                claim.getStatus().name(),
                claim.getClaimAmount(),
                claim.getCreatedAt(),
                claim.getProcessedAt()
        );
    }
}