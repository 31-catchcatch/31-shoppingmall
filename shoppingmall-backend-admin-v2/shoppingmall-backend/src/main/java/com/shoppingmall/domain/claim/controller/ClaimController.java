package com.shoppingmall.domain.claim.controller;

import com.shoppingmall.domain.claim.dto.request.ClaimCreateRequest;
import com.shoppingmall.domain.claim.dto.response.ClaimResponse;
import com.shoppingmall.domain.claim.service.ClaimService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 일반 사용자 교환·환불 신청 API
 */
@RestController
@RequestMapping("/api/v1/orders/{orderId}/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    /**
     * 교환 또는 환불 신청
     *
     * POST /api/v1/orders/{orderId}/claims
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ClaimResponse>> createClaim(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ClaimCreateRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        ClaimResponse response =
                claimService.createClaim(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}