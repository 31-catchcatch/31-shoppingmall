package com.shoppingmall.domain.admin.dto.response;

import com.shoppingmall.domain.coupon.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GET /admin/coupons - 발행된 쿠폰 목록 응답.
 *
 * <p>공개용 CouponResponse 와 달리 "누구 쿠폰인지"를 화면에 그대로 쓸 수 있게 담는다.
 * 관리자가 대신 발행한 쿠폰도 승인된 발행 요청을 거치므로 판매자와 요청 ID가 항상 채워진다.
 */
public record AdminCouponResponse(

        Long couponId,
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
        Long sellerId,
        String sellerName,
        Long couponRequestId,
        LocalDateTime createdAt

) {

    public static AdminCouponResponse from(Coupon coupon) {
        return new AdminCouponResponse(
                coupon.getId(),
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
                coupon.getSeller().getId(),
                coupon.getSeller().getBusinessName(),
                coupon.getCouponRequest().getId(),
                coupon.getCreatedAt()
        );
    }
}
