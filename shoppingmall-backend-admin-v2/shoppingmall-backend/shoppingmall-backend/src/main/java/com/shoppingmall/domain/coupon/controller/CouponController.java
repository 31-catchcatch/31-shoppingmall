package com.shoppingmall.domain.coupon.controller;

import com.shoppingmall.domain.coupon.dto.response.CouponListResponse;
import com.shoppingmall.domain.coupon.service.CouponService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 공통 쿠폰 조회 API
 */
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 현재 사용 가능한 쿠폰 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CouponListResponse>>
    getAvailableCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        CouponListResponse response =
                couponService.getAvailableCoupons(
                        userDetails.getUser().getId(),
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }
}