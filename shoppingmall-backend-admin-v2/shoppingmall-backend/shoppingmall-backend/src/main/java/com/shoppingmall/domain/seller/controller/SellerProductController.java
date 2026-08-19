package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerProductCreateRequest;
import com.shoppingmall.domain.seller.dto.request.SellerProductStatusUpdateRequest;
import com.shoppingmall.domain.seller.dto.request.SellerProductUpdateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerProductDetailResponse;
import com.shoppingmall.domain.seller.dto.response.SellerProductListResponse;
import com.shoppingmall.domain.seller.dto.response.SellerProductResponse;
import com.shoppingmall.domain.seller.service.SellerProductService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 상품 관리 API
 *
 * 담당 API
 * GET    /api/v1/seller/products
 * POST   /api/v1/seller/products
 * PUT    /api/v1/seller/products/{productId}
 * DELETE /api/v1/seller/products/{productId}
 */
@RestController
@RequestMapping("/api/v1/seller/products")
@RequiredArgsConstructor
public class SellerProductController {

    private final SellerProductService sellerProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<SellerProductListResponse>> getMyProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = userDetails.getUser().getId();

        SellerProductListResponse response =
                sellerProductService.getMyProducts(userId, filter, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductDetailResponse>> getProductDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId
    ) {
        Long userId = userDetails.getUser().getId();

        SellerProductDetailResponse response =
                sellerProductService.getProductDetail(userId, productId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SellerProductResponse>> createProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SellerProductCreateRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerProductResponse response =
                sellerProductService.createProduct(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductResponse>> updateProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody SellerProductUpdateRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerProductResponse response =
                sellerProductService.updateProduct(userId, productId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId
    ) {
        Long userId = userDetails.getUser().getId();

        sellerProductService.deleteProduct(userId, productId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<SellerProductResponse>> updateProductStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long productId,
            @Valid @RequestBody SellerProductStatusUpdateRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerProductResponse response =
                sellerProductService.updateProductStatus(userId, productId, request.status());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
