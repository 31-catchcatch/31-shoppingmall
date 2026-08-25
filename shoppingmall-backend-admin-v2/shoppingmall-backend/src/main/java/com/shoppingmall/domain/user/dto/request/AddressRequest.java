package com.shoppingmall.domain.user.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddressRequest {

    @NotBlank(message = "배송지 별칭은 필수입니다.")
    @Size(max = 50)                                                          // [1-6]
    @NoHtml                                                                  // [1-1]
    private String addressName;

    @NotBlank(message = "수령인 이름은 필수입니다.")
    @Size(max = 100)                                                         // [1-6]
    @NoHtml                                                                  // [1-1]
    private String recipientName;

    @NotBlank(message = "수령인 연락처는 필수입니다.")
    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",                      // [1-6]
             message = "연락처 형식이 올바르지 않습니다.")
    private String recipientPhone;

    @NotBlank(message = "주소 정보는 필수입니다.")
    @Size(max = 255)                                                         // [1-6]
    @NoHtml                                                                  // [1-1]
    private String baseAddress;

    @Size(max = 255)                                                         // [1-6]
    @NoHtml                                                                  // [1-1]
    private String detailAddress;

    @NotBlank(message = "우편번호는 필수입니다.")
    @Pattern(regexp = "^\\d{5,6}$", message = "우편번호 형식이 올바르지 않습니다.")  // [1-6]
    private String zipCode;

    private boolean defaultAddress;
}