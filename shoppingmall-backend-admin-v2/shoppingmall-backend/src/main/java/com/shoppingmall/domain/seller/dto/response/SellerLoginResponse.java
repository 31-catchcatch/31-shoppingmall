package com.shoppingmall.domain.seller.dto.response;

/**
 * [4-1 조치 · 3단계] 판매자 로그인 응답 본문. <b>토큰을 담지 않는다.</b>
 *
 * <p>인증은 HttpOnly 쿠키(Set-Cookie)로만 전달된다. 본문에는 판매자 화면이 곧바로 쓸 수 있는
 * 최소 정보만 남긴다.
 */
public record SellerLoginResponse(
        Long sellerId,
        String businessName,
        String role
) {
}
