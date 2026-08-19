package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.product.dto.response.ProductListResponse;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 명세서 "관리자 - 운영 - 상품" 담당.
 * - GET    /admin/products
 * - DELETE /admin/products/{productId}  (강제 제재/삭제)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;

    public PageResponse<ProductListResponse> getProducts(Pageable pageable) {
        Page<ProductListResponse> page = productRepository.findAll(pageable)
                .map(ProductListResponse::from);
        return PageResponse.from(page);
    }

    @Transactional
    public void forceDeleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        product.softDelete();
    }
}
