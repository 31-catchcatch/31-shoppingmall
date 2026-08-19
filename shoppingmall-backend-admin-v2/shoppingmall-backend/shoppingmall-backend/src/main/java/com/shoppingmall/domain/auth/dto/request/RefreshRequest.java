package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken이 필요합니다.") String refreshToken
) {
}
