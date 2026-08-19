package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerSettlementSearchRequest;
import com.shoppingmall.domain.seller.dto.response.SellerSettlementResponse;
import com.shoppingmall.domain.seller.service.SellerSettlementService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 정산 내역 조회 API
 *
 * GET /api/v1/seller/settlements
 */
@RestController
@RequestMapping("/api/v1/seller/settlements")
@RequiredArgsConstructor
public class SellerSettlementController {

    private final SellerSettlementService sellerSettlementService;

    @GetMapping
    public ResponseEntity<ApiResponse<SellerSettlementResponse>> getSettlements(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute SellerSettlementSearchRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerSettlementResponse response =
                sellerSettlementService.getSettlements(userId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
