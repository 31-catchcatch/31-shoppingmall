package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerProfileUpdateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerProfileResponse;
import com.shoppingmall.domain.seller.service.SellerProfileService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** GET/PUT /api/v1/seller/me - 프론트(seller-mypage-edit.js) 대응, 로그인 필요 */
@RestController
@RequestMapping("/api/v1/seller/me")
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SellerProfileResponse response = sellerProfileService.getMyProfile(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SellerProfileUpdateRequest request) {
        sellerProfileService.updateMyProfile(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("판매자 정보가 수정되었습니다.", null));
    }
}
