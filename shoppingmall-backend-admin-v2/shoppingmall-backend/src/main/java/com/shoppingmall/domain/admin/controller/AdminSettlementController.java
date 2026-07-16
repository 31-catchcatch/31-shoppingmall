package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.dto.response.AdminSettlementResponse;
import com.shoppingmall.domain.admin.service.AdminSettlementService;
import com.shoppingmall.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** API 명세서 "관리자 - 정산 - 플랫폼 정산 관리 대시보드" */
@RestController
@RequestMapping("/api/v1/admin/settlements")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminSettlementResponse>> getSettlements(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(adminSettlementService.getSettlements(startDate, endDate)));
    }

    /** PATCH /api/v1/admin/settlements/{settlementId}/complete - 개별 정산 건 완료(지급) 처리 */
    @PatchMapping("/{settlementId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeSettlement(@PathVariable Long settlementId) {
        adminSettlementService.completeSettlement(settlementId);
        return ResponseEntity.ok(ApiResponse.success("정산이 완료 처리되었습니다.", null));
    }
}
