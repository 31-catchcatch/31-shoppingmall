package com.shoppingmall.domain.auth.dto.response;

public record FindAccountResponse(
        String message,
        String maskedUsername // ID 찾기일 때만 값 있음. PASSWORD 요청은 null (메일로만 전달)
) {
    public static FindAccountResponse idFound(String maskedUsername) {
        return new FindAccountResponse("아이디를 찾았습니다.", maskedUsername);
    }

    public static FindAccountResponse passwordReset() {
        return new FindAccountResponse("임시 비밀번호를 이메일로 발송했습니다.", null);
    }
}
