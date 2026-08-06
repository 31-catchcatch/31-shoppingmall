package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** POST /api/v1/auth/find-username - 프론트(find-account.js) 요구 형식: { name, email } */
public record FindUsernameRequest(
        @NotBlank String name,
        @NotBlank @Email String email
) {
}
