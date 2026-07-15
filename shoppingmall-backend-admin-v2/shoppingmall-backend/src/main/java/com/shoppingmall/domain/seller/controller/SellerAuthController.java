package com.shoppingmall.domain.seller.controller;

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
 */
@RestController
@RequestMapping("/api/v1/auth/seller")
@RequiredArgsConstructor
public class SellerAuthController {

    private final SellerAuthService sellerAuthService;

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
}