package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.response.SellerDashboardResponse;
import com.shoppingmall.domain.seller.service.SellerDashboardService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 대시보드 API
 *
 * GET /api/v1/seller/dashboard
 */
@RestController
@RequestMapping("/api/v1/seller/dashboard")
@RequiredArgsConstructor
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> getDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUser().getId();

        SellerDashboardResponse response = sellerDashboardService.getDashboard(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
