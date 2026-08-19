package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /auth/user/find-account, /auth/seller/find-account 공용 요청.
 * type=ID  : name + email 로 아이디 찾기 (마스킹된 아이디 반환)
 * type=PASSWORD : username + email 로 임시 비밀번호 발급 (메일로만 전달, 응답엔 안 담음)
 */
public record FindAccountRequest(
        @NotNull FindAccountType type,

        // [1-2 조치] positive 필터링. type 에 따라 둘 중 하나만 오므로 빈 값(^$)도 허용한다.
        @Pattern(regexp = "^$|^[가-힣A-Za-z ]{2,20}$", message = "이름 형식이 올바르지 않습니다.")
        String name,

        @Pattern(regexp = "^$|^[A-Za-z0-9_-]{4,50}$",
                 message = "아이디는 영문·숫자·언더바·하이픈 4~50자여야 합니다.")
        String username,

        @NotBlank @Email String email
) {
    public enum FindAccountType { ID, PASSWORD }
}
