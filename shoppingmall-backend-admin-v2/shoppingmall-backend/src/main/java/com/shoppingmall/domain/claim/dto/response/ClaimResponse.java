package com.shoppingmall.domain.claim.dto.response;

import com.shoppingmall.domain.claim.entity.Claim;

import java.time.LocalDateTime;

/**
 * 클레임 단건 응답 DTO
 */
public record ClaimResponse(

        Long claimId,
        Long orderDetailId,
        Long orderId,
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
     * Claim Entity를 응답 DTO로 변환한다.
     */
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getOrderDetail().getId(),
                claim.getOrderDetail().getOrder().getId(),
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