package com.shoppingmall.domain.coupon.dto.response;

import com.shoppingmall.domain.coupon.entity.Coupon;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 쿠폰 목록 응답 DTO
 */
public record CouponListResponse(

        List<CouponResponse> coupons,
        int page,
        int size,
        long totalElements,
        int totalPages

) {

    public static CouponListResponse from(
            Page<Coupon> couponPage
    ) {
        return new CouponListResponse(
                couponPage.getContent()
                        .stream()
                        .map(CouponResponse::from)
                        .toList(),
                couponPage.getNumber(),
                couponPage.getSize(),
                couponPage.getTotalElements(),
                couponPage.getTotalPages()
        );
    }
}