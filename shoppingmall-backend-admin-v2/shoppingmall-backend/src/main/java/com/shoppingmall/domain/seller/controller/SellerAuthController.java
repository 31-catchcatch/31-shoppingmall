package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerLoginRequest;
import com.shoppingmall.domain.seller.dto.request.SellerSignupRequest;
import com.shoppingmall.domain.seller.dto.response.SellerLoginResponse;
import com.shoppingmall.domain.seller.dto.response.SellerSignupResponse;
import com.shoppingmall.domain.seller.service.SellerAuthService;
import com.shoppingmall.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 인증 API
 *
 * 담당 API
 * POST /api/v1/auth/seller/signup
 * POST /api/v1/auth/seller/login
 */
@RestController
@RequestMapping("/api/v1/auth/seller")
@RequiredArgsConstructor
public class SellerAuthController {

    private final SellerAuthService sellerAuthService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SellerSignupResponse>> signup(
            @Valid @RequestBody SellerSignupRequest request
    ) {
        SellerSignupResponse response = sellerAuthService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SellerLoginResponse>> login(
            @Valid @RequestBody SellerLoginRequest request
    ) {
        SellerLoginResponse response = sellerAuthService.login(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}