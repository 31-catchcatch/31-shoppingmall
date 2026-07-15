package com.shoppingmall.domain.coupon.dto.response;

import com.shoppingmall.domain.coupon.entity.Coupon;
import com.shoppingmall.domain.coupon.entity.UserCoupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** GET /api/v1/users/me/coupons 목록의 개별 항목 */
public record UserCouponResponse(
        Long userCouponId,
        Long couponId,
        String couponName,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        LocalDateTime validUntil,
        boolean used
) {
    public static UserCouponResponse from(UserCoupon userCoupon) {
        Coupon coupon = userCoupon.getCoupon();
        return new UserCouponResponse(
                userCoupon.getId(),
                coupon.getId(),
                coupon.getCouponName(),
                coupon.getDiscountType().name(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getMaximumDiscountAmount(),
                coupon.getValidUntil(),
                userCoupon.isUsed()
        );
    }
}
