package com.shoppingmall.domain.seller.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 판매자 배송 정보 등록 요청 DTO
 */
public record SellerDeliveryUpdateRequest(

        @NotBlank(message = "택배사를 입력해 주세요.")
        @Size(max = 50, message = "택배사명은 50자 이하여야 합니다.")
        @NoHtml   // [1-1]
        String courierCompany,

        @NotBlank(message = "운송장 번호를 입력해 주세요.")
        @Size(max = 100, message = "운송장 번호는 100자 이하여야 합니다.")
        // [1-1] positive 필터링 - 형식이 고정된 값은 허용 문자만 통과시킨다
        @Pattern(regexp = "^[A-Za-z0-9-]+$",
                 message = "운송장 번호는 영문·숫자·하이픈만 입력할 수 있습니다.")
        String trackingNumber

) {
}