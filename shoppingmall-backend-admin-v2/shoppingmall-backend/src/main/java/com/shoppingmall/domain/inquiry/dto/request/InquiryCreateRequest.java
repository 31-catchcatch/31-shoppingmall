package com.shoppingmall.domain.inquiry.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST /api/v1/customer-center/inquiries
 * 프론트(customercenter.js) 전송 형식: { category, orderNumber, title, content, privacyAgreement }
 */
@Getter
@NoArgsConstructor
public class InquiryCreateRequest {

    @NotBlank(message = "문의 유형을 선택해 주세요.")
    private String category;

    private String orderNumber; // 선택

    @NotBlank(message = "제목을 입력해 주세요.")
    private String title;

    @NotBlank(message = "문의 내용을 입력해 주세요.")
    private String content;

    @AssertTrue(message = "개인정보 수집·이용에 동의해야 문의를 등록할 수 있습니다.")
    private boolean privacyAgreement;
}
