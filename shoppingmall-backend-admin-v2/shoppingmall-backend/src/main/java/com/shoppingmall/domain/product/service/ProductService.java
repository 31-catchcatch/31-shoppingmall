package com.shoppingmall.domain.product.service;

import com.shoppingmall.domain.product.dto.response.ProductDetailResponse;
import com.shoppingmall.domain.product.dto.response.ProductListResponse;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductStatus;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductListResponse> getProducts(Long categoryId, Long brandId, String keyword, Pageable pageable) {
        return productRepository.search(categoryId, brandId, keyword, pageable)
                .map(ProductListResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        // 손님 상세: 판매중지 상품은 목록/검색과 동일하게 "없는 상품"으로 숨긴다(404).
        Product product = productRepository
                .findByIdAndDeletedFalseAndStatus(productId, ProductStatus.ON_SALE)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductDetailResponse.from(product);
    }
}
