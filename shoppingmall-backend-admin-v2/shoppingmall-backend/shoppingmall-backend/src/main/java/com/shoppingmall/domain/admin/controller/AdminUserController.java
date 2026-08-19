package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.dto.request.PointAdjustRequest;
import com.shoppingmall.domain.admin.dto.request.UserStatusUpdateRequest;
import com.shoppingmall.domain.admin.dto.response.AdminUserResponse;
import com.shoppingmall.domain.admin.service.AdminUserService;
import com.shoppingmall.domain.point.dto.response.PointHistoryResponse;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 "관리자 - 운영 - 사용자" 담당.
 * 로그인(POST /admin/users)은 AdminAuthController가 별도로 담당한다 - 여기는 GET/PATCH만.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /** GET /admin/users - 전체 사용자 목록 및 상태/권한 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUsers(pageable)));
    }

    /** GET /admin/users/points - 사용자별 포인트 현황 조회 */
    @GetMapping("/points")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getUserPoints(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUserPoints(pageable)));
    }

    /** PATCH /admin/users/{userId}/points - 특정 사용자 포인트 수동 조정 (가감) */
    @PatchMapping("/{userId}/points")
    public ResponseEntity<ApiResponse<PointHistoryResponse>> adjustPoint(
            @PathVariable Long userId,
            @Valid @RequestBody PointAdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.adjustUserPoint(userId, request)));
    }

    /** PATCH /admin/users/{userId}/status - 계정 정지/정지해제 */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        adminUserService.updateUserStatus(userId, request);
        return ResponseEntity.ok(ApiResponse.success(
                request.suspended() ? "계정이 정지되었습니다." : "정지가 해제되었습니다.", null));
    }
}
