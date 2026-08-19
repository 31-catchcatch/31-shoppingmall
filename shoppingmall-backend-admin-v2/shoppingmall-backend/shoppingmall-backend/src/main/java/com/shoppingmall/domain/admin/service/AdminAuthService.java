package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.auth.dto.request.LoginRequest;
import com.shoppingmall.domain.auth.dto.response.TokenResponse;
import com.shoppingmall.domain.auth.service.AuthService;
import com.shoppingmall.domain.user.entity.Role;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 명세서 "관리자 - 운영 - 관리자 로그인" (POST /api/v1/admin/users).
 *
 * 일반/판매자 로그인과 별도 엔드포인트로 분리해뒀다. 이렇게 하면 Nginx에서
 * "/api/v1/admin/" 경로만 사무실/VPN IP로 제한하는 식의 인프라 단 보호가 가능해진다
 * (같은 URL을 여러 역할이 같이 쓰면 IP 제한을 걸 수 없음).
 *
 * 관리자 회원가입 API는 API 명세서에 없다 - 첫 관리자 계정은 DB에 직접 넣는 것으로 결정됨.
 */
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;   // [3-2 조치]
    private final AuthService authService; // 토큰 발급/저장 로직 재사용

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(request.username())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        // [3-2 조치] 잠금 확인 -> 비밀번호 대조 -> 결과 기록
        loginAttemptService.assertNotLocked(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.onFailure(user.getId());
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        if (user.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.WRONG_LOGIN_ENDPOINT);
        }

        loginAttemptService.onSuccess(user.getId());   // [3-2 조치]
        return authService.issueAndPersistTokens(user);
    }
}
