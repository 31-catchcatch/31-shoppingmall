package com.shoppingmall.domain.auth.dto.request;


public record RefreshRequest(
        // [4-1 조치] 리프레시 토큰은 이제 쿠키로 전달된다. 이 필드는 전환기 폴백이라
        //            비어 있을 수 있으므로 @NotBlank 를 두지 않는다.
        String refreshToken
) {
}
