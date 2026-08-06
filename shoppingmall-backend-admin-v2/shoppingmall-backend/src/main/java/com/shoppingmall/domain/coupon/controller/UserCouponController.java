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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API 명세서 경로(GET /api/v1/users/me/coupons)와 기존 CouponController(GET /api/v1/coupons,
 * 전역 쿠폰 목록)의 경로 불일치를 해소하기 위해 신설. 로그인한 사용자 본인이 보유한 쿠폰만 반환한다.
 */
@RestController
@RequiredArgsConstructor
public class UserCouponController {

    private final UserCouponService userCouponService;

    @GetMapping("/api/v1/users/me/coupons")
    public ResponseEntity<ApiResponse<PageResponse<UserCouponResponse>>> getMyCoupons(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<UserCouponResponse> response =
                userCouponService.getMyAvailableCoupons(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    /** POST /api/v1/coupons/{couponId}/claim - 쿠폰 다운로드(내 지갑으로 발급) */
    @PostMapping("/api/v1/coupons/{couponId}/claim")
    public ResponseEntity<ApiResponse<Void>> claimCoupon(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long couponId) {
        userCouponService.claimCoupon(userDetails.getUser().getId(), couponId);
        return ResponseEntity.ok(ApiResponse.success("쿠폰이 발급되었습니다.", null));
    }
}
