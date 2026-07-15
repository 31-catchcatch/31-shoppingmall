package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST /auth/email-verification 공용 요청.
 * code 가 없으면 "발송 요청", code 가 있으면 "검증 요청" 으로 동작.
 */
public record EmailVerificationRequest(
        @NotBlank @Email String email,
        String code
) {
}
