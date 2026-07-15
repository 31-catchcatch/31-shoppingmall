package com.shoppingmall.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderDeliveryUpdateRequest(

        @NotBlank(message = "택배사를 입력해 주세요.")
        @Size(max = 50)
        String courierCompany,

        @NotBlank(message = "운송장 번호를 입력해 주세요.")
        @Size(max = 100)
        String trackingNumber

) {
}