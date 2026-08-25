package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.dto.request.AdminCouponCreateRequest;
import com.shoppingmall.domain.admin.dto.request.ReviewDecisionRequest;
import com.shoppingmall.domain.admin.dto.response.AdminCouponResponse;
import com.shoppingmall.domain.admin.service.AdminCouponService;
import com.shoppingmall.domain.coupon.dto.response.CouponRequestResponse;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 "관리자 - 운영 - 쿠폰" 담당
 *
 * <p>쿠폰이 만들어지는 경로가 둘이라 이 컨트롤러도 두 갈래다.
 * <ul>
 *   <li>{@code /requests} — 판매자가 낸 발행 요청을 심사한다(기존).
 *   <li>{@code /} — 실제 발행된 쿠폰을 조회하고, 관리자가 직접 발행한다(신규).
 * </ul>
 *
 * <p>⚠️ 클래스 레벨 매핑은 원래 {@code /api/v1/admin/coupons/requests} 였다.
 * 직접 발행 경로를 붙이려고 한 단계 위로 올리면서 기존 두 메서드에 {@code /requests} 를
 * 되붙였다. <b>최종 URL 은 이전과 동일하다</b> — 운영에서 실제로 쓰이는 경로라 바뀌면 안 된다.
 */
@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    /** GET /admin/coupons - 발행된 쿠폰 목록 (판매자 요청분 + 관리자 직접발행분) */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminCouponResponse>>> getIssuedCoupons(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminCouponService.getIssuedCoupons(pageable)));
    }

    /** POST /admin/coupons - 관리자 직접 발행 (판매자 요청 없이 바로 쿠폰을 만든다) */
    @PostMapping
    public ResponseEntity<ApiResponse<AdminCouponResponse>> createCoupon(
            @Valid @RequestBody AdminCouponCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminCouponService.createCoupon(request)));
    }

    /** GET /admin/coupons/requests - 승인 대기 쿠폰 요청 목록 */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<PageResponse<CouponRequestResponse>>> getPendingRequests(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminCouponService.getPendingRequests(pageable)));
    }

    /** PUT /admin/coupons/requests/{requestId} - 쿠폰 승인/반려 처리 */
    @PutMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<CouponRequestResponse>> reviewRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminCouponService.reviewRequest(requestId, request)));
    }
}
