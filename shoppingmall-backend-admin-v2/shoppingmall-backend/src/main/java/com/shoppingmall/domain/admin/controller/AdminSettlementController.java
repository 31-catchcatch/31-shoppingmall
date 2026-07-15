package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.dto.response.AdminSettlementResponse;
import com.shoppingmall.domain.admin.service.AdminSettlementService;
import com.shoppingmall.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
