package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerLoginRequest;
import com.shoppingmall.domain.seller.dto.request.SellerSignupRequest;
import com.shoppingmall.domain.seller.dto.response.SellerLoginResponse;
import com.shoppingmall.domain.seller.dto.response.SellerLoginResult;
import com.shoppingmall.domain.seller.dto.response.SellerSignupResponse;
import com.shoppingmall.domain.seller.service.SellerAuthService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.cookie.AuthCookieFactory;
import jakarta.servlet.http.HttpServletResponse;
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
    private final AuthCookieFactory authCookieFactory;   // [4-1 조치]

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SellerSignupResponse>> signup(
            @Valid @RequestBody SellerSignupRequest request
    ) {
        SellerSignupResponse response = sellerAuthService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * [4-1 조치 · 3단계] 로그인 성공 시 토큰을 HttpOnly 쿠키로만 내려보낸다.
     * 응답 본문에서 토큰을 제거했다(sellerId·businessName·role 만 남김).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SellerLoginResponse>> login(
            @Valid @RequestBody SellerLoginRequest request,
            HttpServletResponse httpResponse
    ) {
        SellerLoginResult result = sellerAuthService.login(request);
        authCookieFactory.writeLoginCookies(
                httpResponse, result.tokens().accessToken(), result.tokens().refreshToken());

        return ResponseEntity.ok(ApiResponse.success(result.toResponse()));
    }
}