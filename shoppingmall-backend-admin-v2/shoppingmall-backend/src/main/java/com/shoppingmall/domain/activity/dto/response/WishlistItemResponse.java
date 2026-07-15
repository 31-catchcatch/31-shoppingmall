package com.shoppingmall.domain.activity.dto.response;

import com.shoppingmall.domain.activity.entity.ProductLike;
import com.shoppingmall.domain.product.entity.Product;

import java.time.LocalDateTime;

/** GET /api/v1/users/me/wishlist 목록의 개별 항목 */
public record WishlistItemResponse(
        Long productId,
        String name,
        int price,
        int discountRate,
        int finalPrice,
        String thumbnailUrl,
        LocalDateTime likedAt
) {
    public static WishlistItemResponse from(ProductLike like) {
        Product product = like.getProduct();
        return new WishlistItemResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getDiscountRate(),
                product.getFinalPrice(),
                product.getThumbnailUrl(),
                like.getCreatedAt()
        );
    }
}
