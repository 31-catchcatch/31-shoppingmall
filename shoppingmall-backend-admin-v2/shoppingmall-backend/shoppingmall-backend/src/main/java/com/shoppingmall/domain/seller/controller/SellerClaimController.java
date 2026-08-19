package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerClaimSearchRequest;
import com.shoppingmall.domain.seller.dto.request.SellerClaimStatusUpdateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerClaimResponse;
import com.shoppingmall.domain.seller.service.SellerClaimService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 클레임 관리 API
 *
 * GET /api/v1/seller/claims
 * PUT /api/v1/seller/claims/{claimId}/status
 */
@RestController
@RequestMapping("/api/v1/seller/claims")
@RequiredArgsConstructor
public class SellerClaimController {

    private final SellerClaimService sellerClaimService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SellerClaimResponse>>> getClaims(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute SellerClaimSearchRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        Page<SellerClaimResponse> response =
                sellerClaimService.getClaims(userId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{claimId}/status")
    public ResponseEntity<ApiResponse<SellerClaimResponse>> updateClaimStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long claimId,
            @Valid @RequestBody SellerClaimStatusUpdateRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerClaimResponse response =
                sellerClaimService.updateClaimStatus(userId, claimId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
