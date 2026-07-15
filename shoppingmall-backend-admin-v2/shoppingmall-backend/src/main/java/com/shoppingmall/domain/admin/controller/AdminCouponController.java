package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.dto.request.ReviewDecisionRequest;
import com.shoppingmall.domain.admin.service.AdminCouponService;
import com.shoppingmall.domain.coupon.dto.response.CouponRequestResponse;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** API 명세서 "관리자 - 운영 - 쿠폰" 담당 */
@RestController
@RequestMapping("/api/v1/admin/coupons/requests")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    /** GET /admin/coupons/requests - 승인 대기 쿠폰 요청 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CouponRequestResponse>>> getPendingRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminCouponService.getPendingRequests(pageable)));
    }

    /** PUT /admin/coupons/requests/{requestId} - 쿠폰 승인/반려 처리 */
    @PutMapping("/{requestId}")
    public ResponseEntity<ApiResponse<CouponRequestResponse>> reviewRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminCouponService.reviewRequest(requestId, request)));
    }
}
