package com.shoppingmall.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** PUT /api/v1/reviews/{reviewId} - 리뷰 수정 요청 */
@Getter
@NoArgsConstructor
public class ReviewUpdateRequest {

    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
    private int rating;

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    private String content;

    private String imageUrl;
}
