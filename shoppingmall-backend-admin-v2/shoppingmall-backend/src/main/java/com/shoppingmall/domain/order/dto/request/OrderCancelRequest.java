package com.shoppingmall.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderCancelRequest(

        @NotBlank(message = "주문 취소 사유를 입력해 주세요.")
        @Size(max = 500)
        String reason

) {
}