package com.shoppingmall.domain.seller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerApplicationCreateRequest(

        @NotBlank(message = "상호명을 입력해 주세요.")
        @Size(max = 100, message = "상호명은 100자 이하여야 합니다.")
        String businessName,

        @NotBlank(message = "사업자등록번호를 입력해 주세요.")
        @Pattern(
                regexp = "^\\d{3}-?\\d{2}-?\\d{5}$",
                message = "사업자등록번호 형식이 올바르지 않습니다."
        )
        String businessRegistrationNumber,

        @NotBlank(message = "대표자명을 입력해 주세요.")
        @Size(max = 50, message = "대표자명은 50자 이하여야 합니다.")
        String representativeName,

        @NotBlank(message = "연락처를 입력해 주세요.")
        @Pattern(
                regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",
                message = "연락처 형식이 올바르지 않습니다."
        )
        String contactNumber,

        @NotBlank(message = "사업장 주소를 입력해 주세요.")
        @Size(max = 255)
        String businessAddress,

        @NotBlank(message = "사업자등록증 파일 URL을 입력해 주세요.")
        @Size(max = 500)
        String businessRegistrationFileUrl,

        @Size(max = 500)
        String mailOrderReportFileUrl

) {
}