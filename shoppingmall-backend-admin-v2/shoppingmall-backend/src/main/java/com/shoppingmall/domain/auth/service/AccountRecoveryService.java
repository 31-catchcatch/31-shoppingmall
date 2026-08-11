package com.shoppingmall.domain.auth.service;

import com.shoppingmall.domain.auth.dto.request.FindAccountRequest;
import com.shoppingmall.domain.auth.dto.response.FindAccountResponse;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * API 명세서 "[일반/판매자] ID/PW 찾기" 담당.
 * TODO(실제 연동 필요): 임시 비밀번호는 실제로는 메일 발송 주체를 통해 사용자에게 전달해야 하지만,
 * 지금은 EmailVerificationService 와 마찬가지로 로그 출력으로 대체한 mock 상태다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager em;

    /** POST /api/v1/auth/find-username - 프론트 요구 방식 { name, email } 로 아이디 찾기 */
    @Transactional(readOnly = true)
    public FindAccountResponse findUsername(com.shoppingmall.domain.auth.dto.request.FindUsernameRequest request) {
        User user = userRepository.findByNameAndEmailAndDeletedFalse(request.name(), request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return FindAccountResponse.idFound(mask(user.getUsername()));
    }

    /**
     * POST /api/v1/auth/reset-password - 사용자가 새 비밀번호를 직접 지정하는 방식 (프론트 화면 이름 유지).
     * 기존 find-account(임시 비밀번호 메일 발송)와 달리, 이메일 인증을 통과한 화면에서 바로 새 비밀번호로 설정한다.
     */
    @Transactional
    public void resetPasswordDirect(com.shoppingmall.domain.auth.dto.request.ResetPasswordRequest request) {
        String sql = "SELECT * FROM users WHERE username = '" + request.username()
                + "' AND email = '" + request.email() + "' AND is_deleted = 0";
        @SuppressWarnings("unchecked")
        List<User> found = em.createNativeQuery(sql, User.class).getResultList();
        User user = found.stream().findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(readOnly = true)
    public FindAccountResponse findUsername(FindAccountRequest request) {
        String sql = "SELECT * FROM users WHERE name = '" + request.name()
                + "' AND email = '" + request.email() + "' AND is_deleted = 0";
        @SuppressWarnings("unchecked")
        List<User> found = em.createNativeQuery(sql, User.class).getResultList();
        User user = found.stream().findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return FindAccountResponse.idFound(user.getUsername());
    }

    @Transactional
    public FindAccountResponse resetPassword(FindAccountRequest request) {
        String sql = "SELECT * FROM users WHERE username = '" + request.username()
                + "' AND email = '" + request.email() + "' AND is_deleted = 0";
        @SuppressWarnings("unchecked")
        List<User> found = em.createNativeQuery(sql, User.class).getResultList();
        User user = found.stream().findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String tempPassword = generateTempPassword();
        user.changePassword(passwordEncoder.encode(tempPassword));

        // 실제 메일 발송 연동 전까지는 로그로 남김
        log.info("[MOCK EMAIL] {} 에게 임시 비밀번호 발송: {}", user.getEmail(), tempPassword);

        return FindAccountResponse.passwordReset();
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(RANDOM.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private String mask(String username) {
        if (username.length() <= 2) {
            return username.charAt(0) + "*".repeat(username.length() - 1);
        }
        int visible = Math.max(2, username.length() / 3);
        return username.substring(0, visible) + "*".repeat(username.length() - visible);
    }
}
