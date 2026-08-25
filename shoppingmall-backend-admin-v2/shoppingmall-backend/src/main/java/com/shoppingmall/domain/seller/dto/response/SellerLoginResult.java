package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.auth.dto.response.TokenResponse;

/**
 * [4-1 조치 · 3단계] <b>내부 전달용</b> 판매자 로그인 결과. 응답 본문으로 직렬화하지 않는다.
 *
 * <p>컨트롤러가 쿠키를 구우려면 토큰이 필요하고, 본문에는 토큰이 나가면 안 된다.
 * 그래서 서비스는 이 객체로 둘 다 넘기고, 컨트롤러가 {@link SellerLoginResponse} 로 좁혀 반환한다.
 */
public record SellerLoginResult(
        TokenResponse tokens,
        Long sellerId,
        String businessName,
        String role
) {
    public SellerLoginResponse toResponse() {
        return new SellerLoginResponse(sellerId, businessName, role);
    }
}
