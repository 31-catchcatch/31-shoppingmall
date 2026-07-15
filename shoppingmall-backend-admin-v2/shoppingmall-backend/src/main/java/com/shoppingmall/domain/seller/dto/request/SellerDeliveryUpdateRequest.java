package com.shoppingmall.domain.seller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 판매자 배송 정보 등록 요청 DTO
 */
public record SellerDeliveryUpdateRequest(

        @NotBlank(message = "택배사를 입력해 주세요.")
        @Size(max = 50, message = "택배사명은 50자 이하여야 합니다.")
        String courierCompany,

        @NotBlank(message = "운송장 번호를 입력해 주세요.")
        @Size(max = 100, message = "운송장 번호는 100자 이하여야 합니다.")
        String trackingNumber

) {
}