package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.dto.request.AdminBannerRequest;
import com.shoppingmall.domain.admin.dto.request.BannerOrderUpdateRequest;
import com.shoppingmall.domain.admin.dto.response.AdminBannerResponse;
import com.shoppingmall.domain.admin.service.AdminBannerService;
import com.shoppingmall.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 명세서 "관리자 - 운영 - 배너" 담당.
 *
 * <p>공개용 GET /api/v1/banners 는 활성 + 기간 내 배너만 돌려주므로 관리자가
 * 중지·기간만료 배너를 볼 수 없었다. 이 컨트롤러가 그 관리 축을 담당한다.
 *
 * <p>경로가 /api/v1/admin/** 이라 SecurityConfig 의 hasRole("ADMIN") 규칙이 그대로 적용된다.
 * (SecurityConfig 수정 불필요)
 */
@RestController
@RequestMapping("/api/v1/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private final AdminBannerService adminBannerService;

    /** GET /admin/banners - 전체 배너 목록 (중지·기간만료 포함, 노출 순서대로) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBannerResponse>>> getBanners() {
        return ResponseEntity.ok(ApiResponse.success(adminBannerService.getBanners()));
    }

    /** POST /admin/banners - 배너 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<AdminBannerResponse>> createBanner(
            @Valid @RequestBody AdminBannerRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("배너가 등록되었습니다.", adminBannerService.createBanner(request)));
    }

    /** PUT /admin/banners/{bannerId} - 배너 수정 */
    @PutMapping("/{bannerId}")
    public ResponseEntity<ApiResponse<AdminBannerResponse>> updateBanner(
            @PathVariable Long bannerId,
            @Valid @RequestBody AdminBannerRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("배너가 수정되었습니다.", adminBannerService.updateBanner(bannerId, request)));
    }

    /** DELETE /admin/banners/{bannerId} - 배너 삭제 (하드 삭제) */
    @DeleteMapping("/{bannerId}")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Long bannerId) {
        adminBannerService.deleteBanner(bannerId);
        return ResponseEntity.ok(ApiResponse.success("배너가 삭제되었습니다.", null));
    }

    /** PATCH /admin/banners/order - 노출 순서 일괄 변경 (한 트랜잭션) */
    @PatchMapping("/order")
    public ResponseEntity<ApiResponse<List<AdminBannerResponse>>> updateOrder(
            @Valid @RequestBody BannerOrderUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("배너 순서가 변경되었습니다.", adminBannerService.updateOrder(request)));
    }
}
