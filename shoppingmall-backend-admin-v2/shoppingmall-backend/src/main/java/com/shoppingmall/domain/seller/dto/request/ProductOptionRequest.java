package com.shoppingmall.domain.seller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 상품 옵션(사이즈 등) 1건. 상품은 최소 1개 이상의 옵션을 가져야 구매(장바구니)가 가능하다. */
public record ProductOptionRequest(

        @NotBlank(message = "옵션명을 입력해 주세요.")
        @Size(max = 100, message = "옵션명은 100자 이하여야 합니다.")
        String optionName,

        @NotNull(message = "추가 금액을 입력해 주세요.")
        @PositiveOrZero(message = "추가 금액은 0원 이상이어야 합니다.")
        Integer additionalPrice,

        @NotNull(message = "재고 수량을 입력해 주세요.")
        @PositiveOrZero(message = "재고 수량은 0 이상이어야 합니다.")
        Integer stockQuantity

) {
}
