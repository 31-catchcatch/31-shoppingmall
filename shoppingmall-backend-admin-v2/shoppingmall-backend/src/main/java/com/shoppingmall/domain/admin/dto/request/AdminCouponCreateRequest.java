package com.shoppingmall.domain.admin.dto.request;

import com.shoppingmall.global.validation.NoHtml;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 관리자 직접 쿠폰 발행 요청. (POST /admin/coupons)
 *
 * <p>판매자 발행 요청(SellerCouponCreateRequest)과 필드·검증 규칙을 일부러 동일하게 맞췄다.
 * 두 경로로 만들어진 쿠폰이 같은 테이블에 들어가므로 규칙이 어긋나면
 * "판매자는 못 넣는 값을 관리자는 넣을 수 있는" 구멍이 생긴다.
 *
 * <p>쿠폰은 반드시 어느 판매자의 것인지가 정해져야 하므로(그 판매자 상품에만 적용된다)
 * 관리자가 대신 발행할 때도 대상 판매자를 지정한다.
 */
public record AdminCouponCreateRequest(

        @NotNull(message = "쿠폰을 발행할 입점업체를 선택해 주세요.")
        Long sellerId,

        @NotBlank(message = "쿠폰명을 입력해 주세요.")
        @Size(max = 100)
        @NoHtml   // [1-1]
        String couponName,

        @NotBlank(message = "할인 방식을 입력해 주세요.")
        @Pattern(regexp = "^(FIXED_AMOUNT|PERCENTAGE)$", message = "할인 방식이 올바르지 않습니다.")
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
        @Positive(message = "발행 수량은 1개 이상이어야 합니다.")
        Integer totalQuantity

) {
}
