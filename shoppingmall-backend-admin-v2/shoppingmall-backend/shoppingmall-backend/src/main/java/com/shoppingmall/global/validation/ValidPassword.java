package com.shoppingmall.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * [3-1 조치] 기관 비밀번호 정책 검증.
 *
 * <p>회원가입·비밀번호 변경·재설정 등 <b>비밀번호를 새로 설정하는 지점에만</b> 부착한다.
 *
 * <p>⚠️ LoginRequest.password 나 currentPassword 에는 절대 부착하지 말 것 —
 * 정책 이전에 가입한 사용자가 로그인도, 비밀번호 변경도 할 수 없게 된다.
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface ValidPassword {

    String message() default "비밀번호는 8자 이상이며 영문 대/소문자·숫자·특수문자를 모두 포함해야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
