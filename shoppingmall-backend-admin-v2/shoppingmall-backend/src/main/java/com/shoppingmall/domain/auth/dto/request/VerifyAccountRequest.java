package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 비밀번호 재설정 1단계: 아이디+이메일 존재 확인용 (값 변경 없음) */
public record VerifyAccountRequest(
        @NotBlank String username,
        @NotBlank @Email String email
) {
}