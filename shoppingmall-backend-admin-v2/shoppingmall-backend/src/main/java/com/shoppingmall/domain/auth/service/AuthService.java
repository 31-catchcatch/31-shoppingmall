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
import com.shoppingmall.global.security.jwt.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;

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

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        // 판매자/관리자 계정은 각각 전용 로그인 경로를 쓰도록 유도
        // (관리자는 특히 Nginx에서 IP 제한을 걸 별도 URL이 필요해서 반드시 분리되어 있어야 함)
        if (user.getRole() == Role.SELLER || user.getRole() == Role.ADMIN) {
            throw new CustomException(ErrorCode.WRONG_LOGIN_ENDPOINT);
        }

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

        return TokenResponse.of(accessToken, refreshToken);
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

        // 탈퇴(정지 포함, is_deleted 재사용) 계정은 재발급 자체를 막는다.
        if (user.isDeleted()) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        return issueAndPersistTokens(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        // 이미 없는 토큰이어도 로그아웃은 성공으로 취급 (멱등)
        refreshTokenRepository.deleteByToken(refreshTokenValue);
    }

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }
}
