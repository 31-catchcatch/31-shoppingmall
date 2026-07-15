package com.shoppingmall.domain.coupon.controller;

import com.shoppingmall.domain.coupon.dto.response.UserCouponResponse;
import com.shoppingmall.domain.coupon.service.UserCouponService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 명세서 경로(GET /api/v1/users/me/coupons)와 기존 CouponController(GET /api/v1/coupons,
 * 전역 쿠폰 목록)의 경로 불일치를 해소하기 위해 신설. 로그인한 사용자 본인이 보유한 쿠폰만 반환한다.
 */
@RestController
@RequestMapping("/api/v1/users/me/coupons")
@RequiredArgsConstructor
public class UserCouponController {

    private final UserCouponService userCouponService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserCouponResponse>>> getMyCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserCouponResponse> response =
                userCouponService.getMyAvailableCoupons(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }
}
