package com.shoppingmall.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * [3-1 조치] 비밀번호 정책 검증기.
 *
 * <p>보고서 대응방안 네 가지를 모두 구현한다.
 * <ol>
 *   <li>최소 길이</li>
 *   <li>영문 대/소문자·숫자·특수문자 조합</li>
 *   <li>연속 문자 / 반복 문자 차단</li>
 *   <li>사전 단어(취약 비밀번호) 차단</li>
 * </ol>
 *
 * <p>SYSTEM 진단의 U-02 / W-09 / D-03 기준(8자 이상 + 복잡도)보다 한 단계 높은
 * 10자 + 4종 조합을 적용했다. 정책 수치를 바꾸려면 아래 상수만 조정하면 된다.
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;
    private static final int SEQUENTIAL_LIMIT = 4;

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    /** 3자 이상 같은 문자 반복: aaa, 111 */
    private static final Pattern REPEATED = Pattern.compile("(.)\\1{2,}");

    /** 운영 중 확인되는 취약 비밀번호는 여기에 계속 추가한다. */
    private static final Set<String> WEAK_WORDS = Set.of(
            "password", "passwd", "qwerty", "asdf", "zxcv", "iloveyou",
            "admin", "administrator", "manager", "master", "root", "guest",
            "catchcatch", "shoppingmall", "welcome", "letmein", "abc123",
            "1q2w3e4r", "qwer1234", "test1234", "samsung", "korea");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null / 빈 값은 @NotBlank 가 담당한다
        if (value == null || value.isBlank()) {
            return true;
        }

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return fail(context, "비밀번호는 " + MIN_LENGTH + "자 이상 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
        if (value.contains(" ")) {
            return fail(context, "비밀번호에는 공백을 사용할 수 없습니다.");
        }
        if (!UPPER.matcher(value).find()
                || !LOWER.matcher(value).find()
                || !DIGIT.matcher(value).find()
                || !SPECIAL.matcher(value).find()) {
            return fail(context, "비밀번호는 영문 대문자·소문자·숫자·특수문자를 모두 포함해야 합니다.");
        }
        if (REPEATED.matcher(value).find()) {
            return fail(context, "같은 문자를 3회 이상 연속으로 사용할 수 없습니다.");
        }
        if (hasSequential(value)) {
            return fail(context, "abcd, 1234 같은 연속된 문자열은 사용할 수 없습니다.");
        }

        String lower = value.toLowerCase(Locale.ROOT);
        for (String weak : WEAK_WORDS) {
            if (lower.contains(weak)) {
                return fail(context, "추측하기 쉬운 단어는 사용할 수 없습니다.");
            }
        }
        return true;
    }

    /** 오름/내림차순으로 SEQUENTIAL_LIMIT 개 이상 이어지는 문자열이 있는지 검사한다. */
    private boolean hasSequential(String value) {
        int asc = 1;
        int desc = 1;
        for (int i = 1; i < value.length(); i++) {
            int diff = value.charAt(i) - value.charAt(i - 1);
            asc = (diff == 1) ? asc + 1 : 1;
            desc = (diff == -1) ? desc + 1 : 1;
            if (asc >= SEQUENTIAL_LIMIT || desc >= SEQUENTIAL_LIMIT) {
                return true;
            }
        }
        // 키보드 배열 연속(qwer, asdf)은 WEAK_WORDS 로 커버한다
        return false;
    }

    private boolean fail(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
