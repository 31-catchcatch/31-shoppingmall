package com.shoppingmall.domain.seller.dto.request;

import com.shoppingmall.global.validation.NoHtml;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerQnaAnswerRequest(

        @NotBlank(message = "답변 내용을 입력해 주세요.")
        @Size(max = 2000, message = "답변은 2,000자 이하여야 합니다.")
        @NoHtml   // [1-1]
        String content

) {
}