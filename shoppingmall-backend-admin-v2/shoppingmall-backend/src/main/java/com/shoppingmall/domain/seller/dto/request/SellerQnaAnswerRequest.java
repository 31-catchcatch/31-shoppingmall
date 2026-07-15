package com.shoppingmall.domain.seller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerQnaAnswerRequest(

        @NotBlank(message = "답변 내용을 입력해 주세요.")
        @Size(max = 2000, message = "답변은 2,000자 이하여야 합니다.")
        String content

) {
}