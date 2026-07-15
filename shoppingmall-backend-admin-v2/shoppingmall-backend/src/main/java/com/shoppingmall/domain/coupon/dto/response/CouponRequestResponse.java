package com.shoppingmall.domain.coupon.dto.response;

import com.shoppingmall.domain.coupon.entity.CouponRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 판매자 쿠폰 발행 요청 응답 DTO
 */
public record CouponRequestResponse(

        Long requestId,
        Long sellerId,
        String couponName,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer totalQuantity,
        String status,
        String rejectionReason,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt

) {

    public static CouponRequestResponse from(
            CouponRequest request
    ) {
        return new CouponRequestResponse(
                request.getId(),
                request.getSeller().getId(),
                request.getCouponName(),
                request.getDiscountType().name(),
                request.getDiscountValue(),
                request.getMinimumOrderAmount(),
                request.getMaximumDiscountAmount(),
                request.getValidFrom(),
                request.getValidUntil(),
                request.getTotalQuantity(),
                request.getStatus().name(),
                request.getRejectionReason(),
                request.getCreatedAt(),
                request.getReviewedAt()
        );
    }
}