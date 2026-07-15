package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.coupon.entity.CouponRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 판매자 쿠폰 발행 요청 응답 DTO
 */
public record SellerCouponResponse(

        Long requestId,
        String couponName,
        String discountType,
        BigDecimal discountValue,
        Integer totalQuantity,
        String approvalStatus,
        LocalDateTime requestedAt

) {

    /**
     * CouponRequest Entity를 판매자 응답 DTO로 변환한다.
     */
    public static SellerCouponResponse from(
            CouponRequest request
    ) {
        return new SellerCouponResponse(
                request.getId(),
                request.getCouponName(),
                request.getDiscountType().name(),
                request.getDiscountValue(),
                request.getTotalQuantity(),
                request.getStatus().name(),
                request.getCreatedAt()
        );
    }
}