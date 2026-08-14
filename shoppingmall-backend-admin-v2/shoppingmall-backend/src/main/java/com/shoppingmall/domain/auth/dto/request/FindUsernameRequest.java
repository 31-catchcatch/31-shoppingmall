package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** POST /api/v1/auth/find-username - 프론트(find-account.js) 요구 형식: { name, email } */
public record FindUsernameRequest(
        @NotBlank
        @Pattern(regexp = "^[가-힣A-Za-z ]{2,20}$", message = "이름 형식이 올바르지 않습니다.")   // [1-2]
        String name,

        @NotBlank @Email String email
) {
}
