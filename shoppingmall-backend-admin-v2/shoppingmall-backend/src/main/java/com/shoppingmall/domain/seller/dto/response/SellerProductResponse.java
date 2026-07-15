package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.product.entity.Product;

import java.time.LocalDateTime;

/**
 * 판매자 상품 단건 응답 DTO
 */
public record SellerProductResponse(

        Long productId,
        Long categoryId,
        String categoryName,
        String productName,
        Integer price,
        Integer discountRate,
        Integer finalPrice,
        String description,
        String thumbnailUrl,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    /**
     * Product 엔티티를 판매자 상품 응답 DTO로 변환한다.
     */
    public static SellerProductResponse from(Product product) {
        return new SellerProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getPrice(),
                product.getDiscountRate(),
                product.getFinalPrice(),
                product.getDescription(),
                product.getThumbnailUrl(),
                product.isDeleted(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}