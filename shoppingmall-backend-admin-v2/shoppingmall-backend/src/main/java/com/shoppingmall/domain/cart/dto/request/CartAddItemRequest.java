package com.shoppingmall.domain.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartAddItemRequest {

    @NotNull(message = "상품 ID는 필수 항목입니다.")
    private Long productId;

    @NotNull(message = "상품 옵션 ID는 필수 항목입니다.")
    private Long productOptionId;

    @Min(value = 1, message = "장바구니에 담을 최소 수량은 1개입니다.")
    private int quantity;
}