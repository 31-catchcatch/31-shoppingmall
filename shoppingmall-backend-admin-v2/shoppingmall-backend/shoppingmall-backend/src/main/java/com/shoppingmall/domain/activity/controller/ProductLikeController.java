package com.shoppingmall.domain.activity.controller;

import com.shoppingmall.domain.activity.dto.response.ProductLikeToggleResponse;
import com.shoppingmall.domain.activity.dto.response.WishlistItemResponse;
import com.shoppingmall.domain.activity.service.ProductLikeService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 "일반 사용자 - 활동" 중 좋아요/위시리스트 매핑.
 * ProductController 의 TODO(POST .../like) 를 이 컨트롤러로 분리 구현.
 */
@RestController
@RequiredArgsConstructor
public class ProductLikeController {

    private final ProductLikeService productLikeService;

    /** POST /api/v1/products/{productId}/like - 좋아요 등록/해제 토글 처리 */
    @PostMapping("/api/v1/products/{productId}/like")
    public ResponseEntity<ApiResponse<ProductLikeToggleResponse>> toggleLike(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId) {
        ProductLikeToggleResponse response = productLikeService.toggleLike(userDetails.getUser().getId(), productId);
        String message = response.liked() ? "좋아요가 등록되었습니다." : "좋아요가 취소되었습니다.";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    /** GET /api/v1/users/me/wishlist - 내 위시리스트 목록 조회 */
    @GetMapping("/api/v1/users/me/wishlist")
    public ResponseEntity<ApiResponse<PageResponse<WishlistItemResponse>>> getWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WishlistItemResponse> response = productLikeService.getWishlist(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }
}
