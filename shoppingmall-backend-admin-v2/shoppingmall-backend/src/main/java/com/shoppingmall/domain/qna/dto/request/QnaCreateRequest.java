package com.shoppingmall.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 일반 사용자의 상품 문의 등록 요청 DTO
 */
public record QnaCreateRequest(

        @NotBlank(message = "문의 제목을 입력해 주세요.")
        @Size(
                max = 200,
                message = "문의 제목은 200자 이하여야 합니다."
        )
        String title,

        @NotBlank(message = "문의 내용을 입력해 주세요.")
        @Size(
                max = 3000,
                message = "문의 내용은 3,000자 이하여야 합니다."
        )
        String content,

        boolean secret

) {
}