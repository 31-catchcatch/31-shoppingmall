package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerCouponCreateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerCouponResponse;
import com.shoppingmall.domain.seller.service.SellerCouponService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 쿠폰 발행 요청 API
 *
 * POST /api/v1/seller/coupons/request
 */
@RestController
@RequestMapping("/api/v1/seller/coupons")
@RequiredArgsConstructor
public class SellerCouponController {

    private final SellerCouponService sellerCouponService;

    @PostMapping("/request")
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
}
