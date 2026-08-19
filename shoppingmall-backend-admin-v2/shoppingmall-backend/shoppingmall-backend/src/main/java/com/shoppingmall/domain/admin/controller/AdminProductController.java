package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.service.AdminProductService;
import com.shoppingmall.domain.product.dto.response.ProductListResponse;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** API 명세서 "관리자 - 운영 - 상품" 담당 */
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    /** GET /admin/products - 관리자 단 상품 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getProducts(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminProductService.getProducts(pageable)));
    }

    /** DELETE /admin/products/{productId} - 상품 강제 제재/삭제 */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> forceDeleteProduct(@PathVariable Long productId) {
        adminProductService.forceDeleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("상품이 강제 삭제 처리되었습니다.", null));
    }
}
