package com.shoppingmall.domain.product.dto.response;

import com.shoppingmall.domain.product.entity.Product;

/** GET /api/v1/products - 카드형 목록에 필요한 필드만 노출 */
public record ProductListResponse(
        Long productId,
        String name,
        int price,
        int discountRate,
        int finalPrice,
        String thumbnailUrl
) {
    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDiscountRate(),
                product.getFinalPrice(),
                product.getThumbnailUrl()
        );
    }
}
