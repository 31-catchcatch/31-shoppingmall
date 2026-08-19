package com.shoppingmall.domain.auth.dto.response;

/**
 * POST /auth/user/verify, /auth/seller/verify 공용 응답.
 * 실제 SMS/PASS 연동 전까지는 항상 ofVerified=true 로 응답하는 mock.
 */
public record VerifyResponse(
        boolean verified,
        String message
) {
    public static VerifyResponse mockSuccess() {
        return new VerifyResponse(true, "본인인증이 완료되었습니다. (mock - 실제 SMS/PASS 연동 전)");
    }
}
