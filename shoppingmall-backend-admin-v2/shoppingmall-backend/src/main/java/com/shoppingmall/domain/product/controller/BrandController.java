package com.shoppingmall.domain.product.controller;

import com.shoppingmall.domain.activity.dto.response.ProductLikeToggleResponse;
import com.shoppingmall.domain.product.dto.response.BrandResponse;
import com.shoppingmall.domain.product.service.BrandService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 명세서 "공통 - 브랜드 목록 조회" + 프론트(brand.js) 요구사항 대응.
 * 목록 조회는 비로그인 허용(SecurityConfig permitAll 필요), 좋아요 토글은 로그인 필요.
 */
@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getBrands() {
        return ResponseEntity.ok(ApiResponse.success(brandService.getBrands()));
    }

    @PostMapping("/{brandId}/like")
    public ResponseEntity<ApiResponse<ProductLikeToggleResponse>> toggleLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long brandId) {
        ProductLikeToggleResponse response = brandService.toggleLike(userDetails.getUser().getId(), brandId);
        String message = response.liked() ? "브랜드 즐겨찾기가 등록되었습니다." : "브랜드 즐겨찾기가 해제되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}
