package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.product.entity.Product;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 판매자가 등록한 상품 목록 응답 DTO
 */
public record SellerProductListResponse(

        List<SellerProductResponse> products,
        int page,
        int size,
        long totalElements,
        int totalPages

) {

    /**
     * Product Page 객체를 상품 목록 응답 DTO로 변환한다.
     */
    public static SellerProductListResponse from(
            Page<Product> productPage
    ) {
        List<SellerProductResponse> products =
                productPage.getContent()
                        .stream()
                        .map(SellerProductResponse::from)
                        .toList();

        return new SellerProductListResponse(
                products,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }
}