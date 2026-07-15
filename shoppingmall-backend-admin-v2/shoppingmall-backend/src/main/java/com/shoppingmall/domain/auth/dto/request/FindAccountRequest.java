package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /auth/user/find-account, /auth/seller/find-account 공용 요청.
 * type=ID  : name + email 로 아이디 찾기 (마스킹된 아이디 반환)
 * type=PASSWORD : username + email 로 임시 비밀번호 발급 (메일로만 전달, 응답엔 안 담음)
 */
public record FindAccountRequest(
        @NotNull FindAccountType type,
        String name,
        String username,
        @NotBlank @Email String email
) {
    public enum FindAccountType { ID, PASSWORD }
}
