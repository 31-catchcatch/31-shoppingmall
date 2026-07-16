package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.auth.dto.request.RefreshRequest;
import com.shoppingmall.domain.auth.service.AuthService;
import com.shoppingmall.domain.seller.dto.request.SellerLoginRequest;
import com.shoppingmall.domain.seller.dto.response.SellerLoginResponse;
import com.shoppingmall.domain.seller.service.SellerAuthService;
import com.shoppingmall.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 인증 API
 *
 * 담당 API
 * POST /api/v1/auth/seller/login
 * POST /api/v1/auth/seller/logout  (API 명세서 v9 신설분 - refresh token 무효화, 일반 사용자 로그아웃과 동일 로직)
 */
@RestController
@RequestMapping("/api/v1/auth/seller")
@RequiredArgsConstructor
public class SellerAuthController {

    private final SellerAuthService sellerAuthService;
    private final AuthService authService;

    /**
     * 판매자 로그인
     *
     * 처리 흐름
     * 1. 로그인 ID와 비밀번호 형식 검증
     * 2. 판매자 인증 서비스 호출
     * 3. Access Token과 Refresh Token 반환
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SellerLoginResponse>> login(
            @Valid
            @RequestBody
            SellerLoginRequest request
    ) {
        SellerLoginResponse response =
                sellerAuthService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    /** POST /api/v1/auth/seller/logout - 판매자 세션 및 토큰 무효화 (멱등) */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다.", null));
    }
}