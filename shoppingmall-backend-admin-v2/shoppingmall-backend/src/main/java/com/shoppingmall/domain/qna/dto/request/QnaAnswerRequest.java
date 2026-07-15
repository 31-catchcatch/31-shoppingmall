package com.shoppingmall.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 판매자의 상품 문의 답변 요청 DTO
 */
public record QnaAnswerRequest(

        @NotBlank(message = "답변 내용을 입력해 주세요.")
        @Size(
                max = 3000,
                message = "답변 내용은 3,000자 이하여야 합니다."
        )
        String content

) {
}