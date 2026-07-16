package com.shoppingmall.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 관리자 1:1 문의 답변 등록 요청 */
public record AdminInquiryAnswerRequest(
        @NotBlank(message = "답변 내용을 입력해주세요.") String content
) {
}
