package com.shoppingmall.domain.product.controller;

import com.shoppingmall.domain.product.dto.response.ProductDetailResponse;
import com.shoppingmall.domain.product.dto.response.ProductListResponse;
import com.shoppingmall.domain.product.service.ProductService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 "일반 사용자 - 상품" 도메인 매핑.
 * 좋아요, 최근 본 상품, 리뷰/문의는 각각 별도 컨트롤러(activity/review/qna)로 분리 예정 - TODO.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ProductListResponse> products = productService.getProducts(categoryId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(products)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable Long productId) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // TODO POST   /api/v1/products/{productId}/like    - 좋아요 토글 (activity 도메인과 연동)
    // TODO GET    /api/v1/products/{productId}/reviews  - review 도메인
    // TODO GET/POST /api/v1/products/{productId}/qna    - qna 도메인
}
