package com.shoppingmall.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * [1-1 조치] HTML 태그 / 스크립트가 포함되지 않은 순수 텍스트만 허용한다.
 *
 * <p>상품명·문의 제목·문의 내용 등 "HTML 이 필요 없는 모든 자유 입력 필드"에 부착한다.
 * 서식 태그가 실제로 필요한 필드(상품 상세 설명 등)에는 이 제약 대신
 * {@link HtmlSanitizer} 를 서비스 계층에서 적용한다.
 *
 * <p>⚠️ 비밀번호 필드에는 절대 부착하지 말 것 — 특수문자를 포함한 정상 비밀번호가 거부된다.
 */
@Documented
@Constraint(validatedBy = NoHtmlValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface NoHtml {

    String message() default "HTML 태그나 스크립트는 입력할 수 없습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
