package com.shoppingmall.domain.admin.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotBlank;

/** 관리자 1:1 문의 답변 등록 요청 */
public record AdminInquiryAnswerRequest(
        @NotBlank(message = "답변 내용을 입력해주세요.")
        @Size(max = 5000, message = "답변은 5,000자 이하여야 합니다.")   // [1-6]
        @NoHtml                                                        // [1-1]
        String content
) {
}
