package com.shoppingmall.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddressRequest {

    @NotBlank(message = "배송지 별칭은 필수입니다.")
    private String addressName;

    @NotBlank(message = "수령인 이름은 필수입니다.")
    private String recipientName;

    @NotBlank(message = "수령인 연락처는 필수입니다.")
    private String recipientPhone;

    @NotBlank(message = "주소 정보는 필수입니다.")
    private String baseAddress;

    private String detailAddress;

    private boolean defaultAddress;
}