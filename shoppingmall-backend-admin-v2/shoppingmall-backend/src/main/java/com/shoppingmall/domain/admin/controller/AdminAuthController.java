package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.service.AdminAuthService;
import com.shoppingmall.domain.auth.dto.request.LoginRequest;
import com.shoppingmall.domain.auth.dto.response.TokenResponse;
import com.shoppingmall.global.common.ApiResponse;
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

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminAuthService.login(request)));
    }
}
