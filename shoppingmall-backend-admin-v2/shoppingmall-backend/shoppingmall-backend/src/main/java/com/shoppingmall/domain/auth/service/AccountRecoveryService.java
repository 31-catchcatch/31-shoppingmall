package com.shoppingmall.domain.auth.service;

import com.shoppingmall.domain.auth.dto.request.FindAccountRequest;
import com.shoppingmall.domain.auth.dto.request.FindUsernameRequest;
import com.shoppingmall.domain.auth.dto.request.ResetPasswordRequest;
import com.shoppingmall.domain.auth.dto.request.VerifyAccountRequest;
import com.shoppingmall.domain.auth.dto.response.FindAccountResponse;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * API 명세서 "[일반/판매자] ID/PW 찾기" 담당.
 *
 * <p><b>[1-2 조치]</b> 기존에는 EntityManager 로 문자열을 이어붙인 네이티브 SQL 을 실행해
 * username/name/email 파라미터에 SQL Injection 이 성립했다. 모든 조회를 Spring Data JPA
 * 파생 쿼리(PreparedStatement 바인딩)로 교체하고, 같은 실수가 재발하지 않도록
 * EntityManager 필드 자체를 제거했다.
 *
 * <p><b>[5-2 조치]</b> 아이디 찾기 응답은 두 경로 모두 마스킹된 아이디를 반환한다.
 *
 * <p>TODO(실제 연동 필요): 임시 비밀번호는 실제로는 메일 발송 주체를 통해 사용자에게 전달해야 하지만,
 * 지금은 EmailVerificationService 와 마찬가지로 로그 출력으로 대체한 mock 상태다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    /** [3-1 조치] 임시 비밀번호도 기관 정책(4종 조합)을 만족하도록 문자군을 나눠 관리한다. */
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT = "23456789";
    private static final String SPECIAL = "!@#$%^&*";
    private static final int TEMP_PASSWORD_LENGTH = 14;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** POST /api/v1/auth/find-username - 프론트 요구 방식 { name, email } 로 아이디 찾기 */
    @Transactional(readOnly = true)
    public FindAccountResponse findUsername(FindUsernameRequest request) {
        User user = userRepository
                .findByNameAndEmailAndDeletedFalse(request.name(), request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return FindAccountResponse.idFound(mask(user.getUsername()));
    }

    /**
     * POST /api/v1/auth/verify-account
     * 1단계: 아이디+이메일이 DB 에 존재하는지 확인만 한다 (비밀번호는 바꾸지 않음).
     */
    @Transactional(readOnly = true)
    public void verifyAccount(VerifyAccountRequest request) {
        userRepository
                .findByUsernameAndEmailAndDeletedFalse(request.username(), request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * POST /api/v1/auth/reset-password - 사용자가 새 비밀번호를 직접 지정하는 방식.
     *
     * <p>⚠️ 남은 과제(항목 4-2 연계): 이 경로는 아직 이메일 인증 완료 여부를 검증하지 않는다.
     * username + email 만 알면 비밀번호를 바꿀 수 있으므로, verify-account 단계에서 발급한
     * 1회용 재설정 토큰을 함께 검증하도록 보완이 필요하다.
     */
    @Transactional
    public void resetPasswordDirect(ResetPasswordRequest request) {
        User user = userRepository
                .findByUsernameAndEmailAndDeletedFalse(request.username(), request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    /** POST /api/v1/auth/user/find-account (type=ID) */
    @Transactional(readOnly = true)
    public FindAccountResponse findUsername(FindAccountRequest request) {
        User user = userRepository
                .findByNameAndEmailAndDeletedFalse(request.name(), request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // [5-2 조치] 원문 아이디를 그대로 반환하던 것을 마스킹으로 통일한다.
        return FindAccountResponse.idFound(mask(user.getUsername()));
    }

    /** POST /api/v1/auth/user/find-account (type=PW) */
    @Transactional
    public FindAccountResponse resetPassword(FindAccountRequest request) {
        User user = userRepository
                .findByUsernameAndEmailAndDeletedFalse(request.username(), request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String tempPassword = generateTempPassword();
        user.changePassword(passwordEncoder.encode(tempPassword));

        // 실제 메일 발송 연동 전까지는 로그로 남김
        log.info("[MOCK EMAIL] {} 에게 임시 비밀번호 발송: {}", user.getEmail(), tempPassword);

        return FindAccountResponse.passwordReset();
    }

    /**
     * [3-1 조치] 각 문자군을 최소 1자씩 포함하는 임시 비밀번호를 만든다.
     * 혼동하기 쉬운 문자(0, O, 1, l, I)는 문자 풀에서 제외했다.
     */
    private String generateTempPassword() {
        List<Character> chars = new ArrayList<>(TEMP_PASSWORD_LENGTH);
        chars.add(pick(UPPER));
        chars.add(pick(LOWER));
        chars.add(pick(DIGIT));
        chars.add(pick(SPECIAL));

        String all = UPPER + LOWER + DIGIT + SPECIAL;
        while (chars.size() < TEMP_PASSWORD_LENGTH) {
            chars.add(pick(all));
        }
        Collections.shuffle(chars, RANDOM);

        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (Character c : chars) {
            sb.append(c.charValue());
        }
        return sb.toString();
    }

    private char pick(String pool) {
        return pool.charAt(RANDOM.nextInt(pool.length()));
    }

    private String mask(String username) {
        if (username.length() <= 2) {
            return username.charAt(0) + "*".repeat(username.length() - 1);
        }
        int visible = Math.max(2, username.length() / 3);
        return username.substring(0, visible) + "*".repeat(username.length() - visible);
    }
}
