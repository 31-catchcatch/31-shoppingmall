package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/auth/reset-password
 * 프론트(find-account.js) 제안 형식은 { userId, newPassword } 였으나,
 * userId만으로 비밀번호를 바꾸게 하면 타인 계정 탈취가 가능해 보안상 위험하다.
 * 이미 같은 화면에서 수집하는 username + email 로 본인 확인을 함께 요구한다.
 * (프론트는 userId 대신 username/email을 담도록 소폭 수정 필요 - 화면 변경은 없음)
 */
public record ResetPasswordRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        String newPassword
) {
}
