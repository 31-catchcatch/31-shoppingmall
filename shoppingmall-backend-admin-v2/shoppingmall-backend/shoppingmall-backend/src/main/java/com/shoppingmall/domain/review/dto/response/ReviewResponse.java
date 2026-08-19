package com.shoppingmall.domain.review.dto.response;

import com.shoppingmall.domain.review.entity.Review;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class ReviewResponse {

    private final Long reviewId;
    private final String reviewerName; // 작성자 이름 혹은 마스킹 처리된 이름
    private final int rating;
    private final String content;
    private final String imageUrl;
    private final LocalDateTime createdAt;

    @Builder
    public ReviewResponse(Long reviewId, String reviewerName, int rating,
                          String content, String imageUrl, LocalDateTime createdAt) {
        this.reviewId = reviewId;
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getId())
                .reviewerName(review.getUser().getName()) // 수동 노출용 실명 파싱
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .build();
    }
}