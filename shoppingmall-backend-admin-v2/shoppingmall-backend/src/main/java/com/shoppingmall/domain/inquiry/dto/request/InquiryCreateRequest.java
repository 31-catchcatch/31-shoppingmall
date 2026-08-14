package com.shoppingmall.domain.inquiry.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

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
    @Pattern(regexp = "^[A-Za-z0-9_-]{2,30}$", message = "허용되지 않은 문의 유형입니다.")   // [1-1]
    private String category;

    // 선택
    @Pattern(regexp = "^$|^[A-Za-z0-9-]{1,50}$", message = "주문번호 형식이 올바르지 않습니다.")  // [1-1]
    private String orderNumber;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")   // [1-6]
    @NoHtml                                                      // [1-1]
    private String title;

    @NotBlank(message = "문의 내용을 입력해 주세요.")
    @Size(max = 5000, message = "문의 내용은 5,000자 이하여야 합니다.")   // [1-6]
    @NoHtml                                                             // [1-1]
    private String content;

    @AssertTrue(message = "개인정보 수집·이용에 동의해야 문의를 등록할 수 있습니다.")
    private boolean privacyAgreement;
}
