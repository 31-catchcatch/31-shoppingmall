package com.shoppingmall.domain.auth.dto.response;

public record EmailVerificationResponse(
        boolean sent,
        boolean verified,
        String message
) {
    public static EmailVerificationResponse ofSent() {
        return new EmailVerificationResponse(true, false, "인증번호를 발송했습니다.");
    }

    public static EmailVerificationResponse ofVerified() {
        return new EmailVerificationResponse(false, true, "이메일 인증이 완료되었습니다.");
    }
}
