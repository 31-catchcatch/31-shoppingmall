package com.shoppingmall.domain.auth.dto.request;

import com.shoppingmall.global.validation.ValidPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        // [1-2 조치] positive 필터링. 아이디 찾기·비밀번호 재설정 API 의 @Pattern 과 규칙이 일치해야 한다.
        //            (여기만 느슨하면 특수문자 아이디로 가입한 계정이 복구 불가가 된다)
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_-]{4,50}$",
                 message = "아이디는 영문·숫자·언더바·하이픈 4~50자여야 합니다.")
        String username,

        // [3-1 조치] 길이만 보던 검증을 기관 정책(10자 + 4종 조합 + 연속/반복/사전단어 차단)으로 교체
        @NotBlank
        @ValidPassword
        String password,

        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[가-힣A-Za-z ]{2,20}$", message = "이름 형식이 올바르지 않습니다.")   // [1-1]
        String name,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Pattern(regexp = "\\d{9,11}", message = "전화번호는 하이픈 없이 숫자만 입력해주세요.") String phoneNumber
) {
}
