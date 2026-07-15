package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.") String password,
        @NotBlank @Size(max = 20) String name,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Pattern(regexp = "\\d{9,11}", message = "전화번호는 하이픈 없이 숫자만 입력해주세요.") String phoneNumber
) {
}
