package com.shoppingmall.domain.coupon.dto.request;

import com.shoppingmall.domain.coupon.entity.CouponRequestStatus;

/**
 * 쿠폰 또는 쿠폰 요청 목록 검색 조건
 */
public record CouponSearchRequest(

        CouponRequestStatus status,
        Long sellerId,
        Boolean active,
        Integer page,
        Integer size

) {
}