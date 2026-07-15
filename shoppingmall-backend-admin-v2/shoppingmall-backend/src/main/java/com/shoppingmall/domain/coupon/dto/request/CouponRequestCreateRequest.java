package com.shoppingmall.domain.coupon.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 쿠폰 발행 요청 공통 DTO
 */
public record CouponRequestCreateRequest(

        @NotBlank(message = "쿠폰명을 입력해 주세요.")
        @Size(max = 100)
        String couponName,

        @NotBlank(message = "할인 방식을 입력해 주세요.")
        String discountType,

        @NotNull(message = "할인 값을 입력해 주세요.")
        @Positive(message = "할인 값은 0보다 커야 합니다.")
        BigDecimal discountValue,

        @PositiveOrZero
        BigDecimal minimumOrderAmount,

        @PositiveOrZero
        BigDecimal maximumDiscountAmount,

        @NotNull(message = "쿠폰 시작일을 입력해 주세요.")
        LocalDateTime validFrom,

        @NotNull(message = "쿠폰 종료일을 입력해 주세요.")
        LocalDateTime validUntil,

        @NotNull(message = "발행 수량을 입력해 주세요.")
        @Positive(message = "발행 수량은 1 이상이어야 합니다.")
        Integer totalQuantity

) {
}