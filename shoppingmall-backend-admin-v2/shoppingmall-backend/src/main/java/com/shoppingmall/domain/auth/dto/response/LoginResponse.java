package com.shoppingmall.domain.auth.dto.response;

/**
 * [4-1 조치] 로그인 응답 본문. <b>토큰을 담지 않는다.</b>
 *
 * <p>인증은 HttpOnly 쿠키(Set-Cookie)로만 전달되며, 본문에는 화면이 곧바로 쓸 수 있는
 * 최소 식별정보만 남긴다. 프론트는 이 값 없이도 동작하지만(역할 판정은 {@code GET /users/me}),
 * 로그인 직후 왕복을 한 번 줄일 수 있고 운영 중 응답만 보고 누구로 로그인됐는지 확인할 수 있다.
 */
public record LoginResponse(
        Long userId,
        String role,
        String name
) {
    public static LoginResponse from(TokenResponse tokens) {
        return new LoginResponse(tokens.userId(), tokens.role(), tokens.name());
    }
}
