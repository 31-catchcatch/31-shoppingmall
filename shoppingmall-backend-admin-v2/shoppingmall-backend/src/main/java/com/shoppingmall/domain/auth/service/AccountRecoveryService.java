package com.shoppingmall.domain.auth.service;

import com.shoppingmall.domain.auth.dto.request.FindAccountRequest;
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

/**
 * API 명세서 "[일반/판매자] ID/PW 찾기" 담당.
 * TODO(실제 연동 필요): 임시 비밀번호는 실제로는 메일 발송 업체를 통해 사용자에게 전달해야 하는데,
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

    @Transactional(readOnly = true)
    public FindAccountResponse findUsername(FindAccountRequest request) {
        User user = userRepository.findByNameAndEmailAndDeletedFalse(request.name(), request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return FindAccountResponse.idFound(mask(user.getUsername()));
    }

    @Transactional
    public FindAccountResponse resetPassword(FindAccountRequest request) {
        User user = userRepository.findByUsernameAndEmailAndDeletedFalse(request.username(), request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String tempPassword = generateTempPassword();
        user.changePassword(passwordEncoder.encode(tempPassword));

        // 실제 메일 발송 연동 전까지는 로그로 대체
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
