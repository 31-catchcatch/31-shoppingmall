package com.shoppingmall.domain.auth.service;

import com.shoppingmall.domain.auth.dto.request.LoginRequest;
import com.shoppingmall.domain.auth.dto.request.SignupRequest;
import com.shoppingmall.domain.auth.dto.response.SignupResponse;
import com.shoppingmall.domain.auth.dto.response.TokenResponse;
import com.shoppingmall.domain.auth.entity.RefreshToken;
import com.shoppingmall.domain.auth.repository.RefreshTokenRepository;
import com.shoppingmall.domain.user.entity.Role;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.security.LoginAttemptService;
import com.shoppingmall.global.security.jwt.JwtTokenProvider;
import com.shoppingmall.global.security.jwt.TokenInvalidationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * API 명세서 "공통/인증" 도메인 중 일반 사용자 로그인/회원가입/토큰재발급/로그아웃.
 * 판매자 로그인/회원가입은 SellerAuthService 에서 동일 패턴으로 처리.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;   // [3-2 조치]
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenInvalidationRegistry tokenInvalidationRegistry;   // [4-2 조치]

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .role(Role.USER)
                .build();

        User saved = userRepository.save(user);
        return new SignupResponse(saved.getId(), saved.getUsername());
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(request.username())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        // 아이디는 대소문자를 구분해 로그인한다. DB 조회는 collation 상 대소문자를 무시하므로
        // 저장된 값과 입력값의 대소문자가 정확히 같은지 여기서 확정한다.
        // (중복 가입 방지는 existsByUsername 이 대소문자 무시로 계속 막는다 — 의도된 비대칭)
        // 존재하지 않는 아이디와 동일하게 취급하기 위해 잠금 검사 전에 판정한다.
        if (!user.getUsername().equals(request.username())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        // [3-2 조치] 잠금 확인 -> 비밀번호 대조 -> 결과 기록
        loginAttemptService.assertNotLocked(user);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.onFailure(user.getId());
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        // 판매자/관리자 계정은 각각 전용 로그인 경로를 쓰도록 유도
        // (관리자는 특히 Nginx에서 IP 제한을 걸 별도 URL이 필요해서 반드시 분리되어 있어야 함)
        if (user.getRole() == Role.SELLER || user.getRole() == Role.ADMIN) {
            throw new CustomException(ErrorCode.WRONG_LOGIN_ENDPOINT);
        }

        loginAttemptService.onSuccess(user.getId());   // [3-2 조치]
        return issueAndPersistTokens(user);
    }

    /** SellerAuthService 와 공유하는 토큰 발급 로직. */
    @Transactional
    public TokenResponse issueAndPersistTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getRole().name());

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusNanos(jwtTokenProvider.getRefreshTokenValidityMs() * 1_000_000);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(expiresAt)
                .build());

        return TokenResponse.of(user, accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        if (savedToken.isExpired()) {
            refreshTokenRepository.delete(savedToken);
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        User user = savedToken.getUser();

        // Refresh Token Rotation: 재발급할 때마다 기존 토큰은 폐기하고 새 토큰으로 교체 (탈취 대비)
        refreshTokenRepository.delete(savedToken);

        // [4-1 조치] 삭제를 먼저 DB 에 반영한다.
        //
        // 과거에는 같은 초에 발급과 재발급이 겹치면 sub·role·iat·exp 가 모두 같아 서명까지 동일한
        // 토큰 문자열이 만들어졌고, refresh_tokens.token 이 UNIQUE 라 409 가 났다.
        // Hibernate 가 같은 트랜잭션에서 INSERT 를 DELETE 보다 먼저 내보내기 때문이다.
        //
        // 지금은 Refresh Token 에 jti 가 들어가 같은 문자열이 만들어지지 않으므로 그 충돌은 사라졌다.
        // 다만 삭제를 먼저 반영하는 순서 자체는 유지한다 — INSERT/DELETE 순서에 의존하지 않는 편이 안전하다.
        refreshTokenRepository.flush();

        // 탈퇴(정지 포함, is_deleted 재사용) 계정은 재발급 자체를 막는다.
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        return issueAndPersistTokens(user);
    }

    /**
     * [4-2 조치] 로그아웃 — 발급된 Access Token 까지 무효화한다.
     *
     * <p>기존에는 Refresh Token 만 삭제해서, 이미 발급된 Access Token 이 남은 유효기간(15분)
     * 동안 그대로 통했다. 탈취된 토큰을 사용자가 로그아웃으로 끊을 방법이 없었다.
     * 무효화 기준 시각을 남기면 인증 필터가 그 이전 발급분을 전부 거부한다.
     *
     * <p>비인증 호출(토큰 없이 요청)도 성공으로 취급한다 — 로그아웃은 멱등해야 한다.
     */
    @Transactional
    public void logout(Long userId) {
        terminateAllSessions(userId);
    }

    /**
     * [4-2 조치] 이 사용자의 <b>모든 세션</b>을 끊는다. 로그아웃과 비밀번호 변경이 함께 쓴다.
     *
     * <p>반드시 <b>두 가지를 같이</b> 해야 한다. Access Token 만 무효로 만들면
     * 클라이언트가 401 을 받고 곧바로 {@code POST /auth/refresh} 를 부르는데,
     * Refresh 행이 남아 있으면 <b>새 Access Token 이 발급되어 무효화가 우회된다</b>
     * (새 토큰의 iat 가 무효화 기준 시각보다 나중이라 검사를 통과한다).
     *
     * <ol>
     *   <li>Access Token — 메모리 레지스트리에 무효화 기준 시각 기록
     *   <li>Refresh Token — DB 행 삭제로 재발급 경로 차단
     * </ol>
     */
    @Transactional
    public void terminateAllSessions(Long userId) {
        if (userId == null) {
            return;
        }
        tokenInvalidationRegistry.invalidate(userId);

        userRepository.findById(userId)
                .ifPresent(refreshTokenRepository::deleteAllByUser);
    }

    /** 구버전 클라이언트 호환 — 본문으로 refreshToken 을 보내는 경로. */
    @Transactional
    public void logoutByRefreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        // [4-2 조치] Access Token 이 요청에 실려 오지 않으면 위 logout(userId) 이 아무 일도
        // 하지 않는다. 그 경우 이 경로만 타는데, 여기서 Refresh 행만 지우면 Access Token 은
        // 남은 유효기간 동안 계속 통한다. Refresh Token 으로 사용자를 특정할 수 있으므로
        // 여기서도 무효화한다.
        refreshTokenRepository.findByToken(refreshTokenValue)
                .ifPresent(refreshToken ->
                        tokenInvalidationRegistry.invalidate(refreshToken.getUser().getId()));

        refreshTokenRepository.deleteByToken(refreshTokenValue);
    }

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
}
