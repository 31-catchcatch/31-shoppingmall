package com.shoppingmall.domain.auth.dto.response;

public record TokenResponse(
        String grantType,
        String accessToken,
        String refreshToken
) {
    public static TokenResponse of(String accessToken, String refreshToken) {
        return new TokenResponse("Bearer", accessToken, refreshToken);
    }
}
