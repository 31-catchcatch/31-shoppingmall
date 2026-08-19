package com.shoppingmall.domain.order.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderDeliveryUpdateRequest(

        @NotBlank(message = "택배사를 입력해 주세요.")
        @Size(max = 50)
        @NoHtml   // [1-1]
        String courierCompany,

        @NotBlank(message = "운송장 번호를 입력해 주세요.")
        @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9-]+$",                                        // [1-1]
                 message = "운송장 번호는 영문·숫자·하이픈만 입력할 수 있습니다.")
        String trackingNumber

) {
}