package com.shoppingmall.domain.user.dto.request;

import com.shoppingmall.global.validation.ValidPassword;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {

    private String currentPassword; // 기존 비밀번호 검증용

    // [3-1 조치] 개별 정규식 대신 공통 정책(@ValidPassword)으로 통일한다.
    //            currentPassword 에는 부착하지 않는다 - 정책 이전 가입자가 변경 자체를 못 하게 된다.
    @ValidPassword
    private String newPassword; // 새 비밀번호

    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    @Size(max = 20)                                                     // [1-6]
    @Pattern(regexp = "^[가-힣A-Za-z ]{2,20}$", message = "이름 형식이 올바르지 않습니다.")   // [1-1]
    private String name;

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)")
    private String phoneNumber;
}