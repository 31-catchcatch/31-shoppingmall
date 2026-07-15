package com.shoppingmall.domain.auth.dto.response;

public record SignupResponse(
        Long userId,
        String username
) {
}
