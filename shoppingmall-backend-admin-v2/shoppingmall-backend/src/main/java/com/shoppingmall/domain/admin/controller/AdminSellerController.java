package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.dto.request.ReviewDecisionRequest;
import com.shoppingmall.domain.admin.dto.request.SellerStatusUpdateRequest;
import com.shoppingmall.domain.admin.service.AdminSellerService;
import com.shoppingmall.domain.seller.dto.response.SellerApplicationResponse;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API 명세서 "관리자 - 운영 - 판매자/입점" 담당 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminSellerController {

    private final AdminSellerService adminSellerService;

    /** GET /admin/sellers/applications - 입점 신청 목록 (기본: PENDING만) */
    @GetMapping("/sellers/applications")
    public ResponseEntity<ApiResponse<List<SellerApplicationResponse>>> getApplications(
            @RequestParam(required = false) SellerApplicationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(adminSellerService.getApplications(status)));
    }

    /** POST /admin/sellers/applications/{appId}/status - 입점 승인/반려 */
    @PostMapping("/sellers/applications/{appId}/status")
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> reviewApplication(
            @PathVariable Long appId,
            @Valid @RequestBody ReviewDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminSellerService.reviewApplication(appId, request)));
    }

    /** PUT /admin/sellers/{sellerId}/status - 입점 업체 상태 관리 (정상/정지/폐점) */
    @PutMapping("/sellers/{sellerId}/status")
    public ResponseEntity<ApiResponse<Void>> updateSellerStatus(
            @PathVariable Long sellerId,
            @Valid @RequestBody SellerStatusUpdateRequest request) {
        adminSellerService.updateSellerStatus(sellerId, request);
        return ResponseEntity.ok(ApiResponse.success("판매자 상태가 변경되었습니다.", null));
    }
}
