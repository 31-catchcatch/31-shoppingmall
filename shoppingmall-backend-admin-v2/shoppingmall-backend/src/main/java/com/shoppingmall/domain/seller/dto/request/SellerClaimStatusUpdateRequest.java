package com.shoppingmall.domain.seller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerClaimStatusUpdateRequest(

        @NotBlank(message = "변경할 클레임 상태를 입력해 주세요.")
        String status,

        @Size(max = 500, message = "처리 사유는 500자 이하여야 합니다.")
        String reason

) {
}