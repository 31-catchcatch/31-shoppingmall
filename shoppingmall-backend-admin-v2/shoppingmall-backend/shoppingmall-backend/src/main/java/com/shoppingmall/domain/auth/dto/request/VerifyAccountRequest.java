package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 비밀번호 재설정 1단계: 아이디+이메일 존재 확인용 (값 변경 없음) */
public record VerifyAccountRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{4,50}$",                                     // [1-2]
                 message = "아이디는 영문·숫자·언더바·하이픈 4~50자여야 합니다.")
        String username,

        @NotBlank @Email String email
) {
}