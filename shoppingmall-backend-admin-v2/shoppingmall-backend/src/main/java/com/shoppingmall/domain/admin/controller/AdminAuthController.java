package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.service.AdminAuthService;
import com.shoppingmall.domain.auth.dto.request.LoginRequest;
import com.shoppingmall.domain.auth.dto.response.LoginResponse;
import com.shoppingmall.domain.auth.dto.response.TokenResponse;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.cookie.AuthCookieFactory;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 명세서 "관리자 - 운영 - 관리자 로그인" (POST /api/v1/admin/users).
 *
 * ⚠️ 배포 시 Nginx에서 "/api/v1/admin/" 경로 전체를 사무실/VPN IP로 제한할 것 (README 참고).
 * 로그인 전이라 토큰이 없으므로 이 엔드포인트만 SecurityConfig에서 permitAll 처리되어 있다.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final AuthCookieFactory authCookieFactory;   // [4-1 조치]

    /**
     * [4-1 조치] 관리자도 일반 사용자와 같은 인증 쿠키를 쓴다.
     * 한 브라우저에서 관리자와 일반 계정을 동시에 유지할 수는 없게 되며(마지막 로그인만 유효),
     * 이는 쿠키 통합에 따른 의도된 동작이다.
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletResponse response) {
        TokenResponse tokens = adminAuthService.login(request);
        authCookieFactory.writeLoginCookies(response, tokens.accessToken(), tokens.refreshToken());
        // [4-1 조치 · 3단계] 응답 본문에서 토큰 제거. 인증은 Set-Cookie 로만 전달된다.
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(tokens)));
    }
}
