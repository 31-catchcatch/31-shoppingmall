package com.shoppingmall.domain.auth.dto.response;

import com.shoppingmall.domain.user.entity.User;

/**
 * [4-1 조치] <b>내부 전달용</b> 토큰 묶음. 응답 본문으로 직렬화하지 않는다.
 *
 * <p>3단계에서 인증을 HttpOnly 쿠키로 일원화하면서, 토큰은 컨트롤러가 쿠키를 굽는 데만 쓰고
 * 클라이언트에게는 내려보내지 않는다. 그런데 컨트롤러가 응답 본문에 넣을 최소 식별정보
 * (userId·role·name)를 알아야 하므로, 토큰과 함께 이 객체에 실어 나른다.
 *
 * <p>이 레코드가 컨트롤러 밖(HTTP 응답)으로 나가면 토큰이 다시 노출된다.
 * 컨트롤러는 반드시 {@link LoginResponse} 로 변환해서 반환할 것.
 */
public record TokenResponse(
        String grantType,
        String accessToken,
        String refreshToken,
        Long userId,
        String role,
        String name
) {
    public static TokenResponse of(User user, String accessToken, String refreshToken) {
        return new TokenResponse(
                "Bearer",
                accessToken,
                refreshToken,
                user.getId(),
                user.getRole().name(),
                user.getName()
        );
    }
}
