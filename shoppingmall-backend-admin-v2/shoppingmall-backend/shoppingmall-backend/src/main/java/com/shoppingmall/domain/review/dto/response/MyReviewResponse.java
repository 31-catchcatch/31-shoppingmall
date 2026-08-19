package com.shoppingmall.domain.review.dto.response;

import com.shoppingmall.domain.review.entity.Review;

import java.time.LocalDateTime;

/** GET /api/v1/users/me/reviews 목록의 개별 항목 - 상품 정보 포함 */
public record MyReviewResponse(
        Long reviewId,
        Long productId,
        String productName,
        String productThumbnailUrl,
        int rating,
        String content,
        String imageUrl,
        LocalDateTime createdAt
) {
    public static MyReviewResponse from(Review review) {
        return new MyReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                review.getProduct().getThumbnailUrl(),
                review.getRating(),
                review.getContent(),
                review.getImageUrl(),
                review.getCreatedAt()
        );
    }
}
