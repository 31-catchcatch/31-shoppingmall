package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerSalesSearchRequest;
import com.shoppingmall.domain.seller.dto.response.SellerSalesResponse;
import com.shoppingmall.domain.seller.service.SellerSalesService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 매출 통계 API
 *
 * GET /api/v1/seller/sales
 */
@RestController
@RequestMapping("/api/v1/seller/sales")
@RequiredArgsConstructor
public class SellerSalesController {

    private final SellerSalesService sellerSalesService;

    @GetMapping
    public ResponseEntity<ApiResponse<SellerSalesResponse>> getSales(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute SellerSalesSearchRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerSalesResponse response = sellerSalesService.getSales(userId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
