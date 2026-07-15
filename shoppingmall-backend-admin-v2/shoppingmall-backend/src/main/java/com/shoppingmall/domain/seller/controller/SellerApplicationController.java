package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerApplicationCreateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerApplicationResponse;
import com.shoppingmall.domain.seller.service.SellerApplicationService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 입점 신청 API
 *
 * 담당 API
 * POST /api/v1/seller/applications
 */
@RestController
@RequestMapping("/api/v1/seller/applications")
@RequiredArgsConstructor
public class SellerApplicationController {

    private final SellerApplicationService sellerApplicationService;

    /**
     * 판매자 입점 신청서를 제출한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SellerApplicationResponse>> createApplication(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SellerApplicationCreateRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerApplicationResponse response =
                sellerApplicationService.createApplication(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
