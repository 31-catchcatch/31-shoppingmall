package com.shoppingmall.domain.coupon.dto.response;

import com.shoppingmall.domain.coupon.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 실제 발행 쿠폰 응답 DTO
 */
public record CouponResponse(

        Long couponId,
        Long sellerId,
        String couponName,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer totalQuantity,
        Integer issuedQuantity,
        boolean active,
        LocalDateTime createdAt

) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getSeller().getId(),
                coupon.getCouponName(),
                coupon.getDiscountType().name(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getMaximumDiscountAmount(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getTotalQuantity(),
                coupon.getIssuedQuantity(),
                coupon.isActive(),
                coupon.getCreatedAt()
        );
    }
}