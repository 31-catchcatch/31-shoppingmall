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
 * 좋아요/위시리스트는 activity.ProductLikeController, 리뷰/문의는 review/qna 컨트롤러로 분리되어 있음.
 * 최근 본 상품은 RDB가 아닌 Redis/localStorage로 구현하기로 결정되어 이 백엔드 범위 밖임 (DB 정의서 Overview 참고).
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ProductListResponse> products = productService.getProducts(categoryId, brandId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(products)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable Long productId) {
        ProductDetailResponse response = productService.getProductDetail(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
