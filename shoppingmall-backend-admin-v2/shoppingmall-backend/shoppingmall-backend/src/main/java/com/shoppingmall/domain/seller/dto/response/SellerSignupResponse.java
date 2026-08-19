package com.shoppingmall.domain.seller.dto.response;

/**
 * 판매자 회원가입 응답.
 *
 * 가입 직후에는 아직 승인 전(PENDING)이라 로그인 토큰을 발급하지 않는다.
 * 프론트에는 "가입 완료, 승인 대기중" 안내에 필요한 정보만 내려준다.
 */
public record SellerSignupResponse(

        Long userId,
        String username,
        Long applicationId,
        String applicationStatus

) {
}
