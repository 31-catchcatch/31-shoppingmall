package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerCouponCreateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerCouponResponse;
import com.shoppingmall.domain.seller.service.SellerCouponService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 쿠폰 발행 요청 API
 *
 * POST /api/v1/seller/coupons/request
 * GET  /api/v1/seller/coupons/requests
 */
@RestController
@RequestMapping("/api/v1/seller/coupons")
@RequiredArgsConstructor
public class SellerCouponController {

    private final SellerCouponService sellerCouponService;

    @PostMapping({"/request", "/requests"})
    public ResponseEntity<ApiResponse<SellerCouponResponse>> createCouponRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SellerCouponCreateRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerCouponResponse response =
                sellerCouponService.createCouponRequest(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 로그인한 판매자 본인이 낸 쿠폰 발행 요청 목록을 조회한다.
     * 승인 대기/승인/반려를 모두 내려주고, 화면에서 필요한 상태만 골라 쓴다.
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<PageResponse<SellerCouponResponse>>> getMyCouponRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long userId = userDetails.getUser().getId();

        PageResponse<SellerCouponResponse> response =
                sellerCouponService.getMyCouponRequests(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
