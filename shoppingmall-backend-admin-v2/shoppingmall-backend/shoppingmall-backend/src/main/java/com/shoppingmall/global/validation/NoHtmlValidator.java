package com.shoppingmall.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

/**
 * [1-1 조치] 입력값에서 모든 태그를 제거한 결과가 원본과 같으면 "HTML 없음"으로 판정한다.
 *
 * <p>엔티티(&amp;amp; 등)는 양쪽 모두 디코드해서 비교하므로
 * "5 &lt; 10", "A &amp; B" 같은 정상 입력은 그대로 통과한다.
 */
public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null / 빈 값은 @NotBlank, @NotNull 이 담당한다 (책임 분리)
        if (value == null || value.isBlank()) {
            return true;
        }

        String stripped = Jsoup.clean(value, Safelist.none());

        return Parser.unescapeEntities(stripped, false)
                .equals(Parser.unescapeEntities(value, false));
    }
}
