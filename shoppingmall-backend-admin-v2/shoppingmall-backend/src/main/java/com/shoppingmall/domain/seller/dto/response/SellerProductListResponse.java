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
        int totalPages,
        Counts counts

) {

    /**
     * 판매자 상품 목록 상단의 상태별 개수 요약.
     * 페이지네이션(현재 페이지)과 무관하게 판매자 전체 상품을 기준으로 집계한다.
     * total == onSale + suspended + soldOut (분류는 겹치지 않고 완전하다).
     */
    public record Counts(
            long total,
            long onSale,
            long suspended,
            long soldOut
    ) {
    }

    /**
     * Product Page + 상태별 개수를 상품 목록 응답 DTO로 변환한다.
     */
    public static SellerProductListResponse from(
            Page<Product> productPage,
            Counts counts
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
                productPage.getTotalPages(),
                counts
        );
    }
}