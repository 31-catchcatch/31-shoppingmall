package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerRefundCreateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerRefundResponse;
import com.shoppingmall.domain.seller.service.SellerRefundService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 환불 처리 API
 *
 * POST /api/v1/seller/refunds
 */
@RestController
@RequestMapping("/api/v1/seller/refunds")
@RequiredArgsConstructor
public class SellerRefundController {

    private final SellerRefundService sellerRefundService;

    @PostMapping
    public ResponseEntity<ApiResponse<SellerRefundResponse>> processRefund(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SellerRefundCreateRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerRefundResponse response =
                sellerRefundService.processRefund(userId, request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }
}
