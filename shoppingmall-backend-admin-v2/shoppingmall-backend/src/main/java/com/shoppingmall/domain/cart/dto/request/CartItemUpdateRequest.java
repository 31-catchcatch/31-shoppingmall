package com.shoppingmall.domain.cart.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CartItemUpdateRequest {

    @Min(value = 1, message = "변경할 수량은 최소 1개 이상이어야 합니다.")
    private int quantity;
}