package com.shoppingmall.domain.coupon.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 관리자의 실제 쿠폰 생성 요청
 */
public record CouponCreateRequest(

        @NotNull(message = "쿠폰 요청 ID가 필요합니다.")
        Long couponRequestId

) {
}