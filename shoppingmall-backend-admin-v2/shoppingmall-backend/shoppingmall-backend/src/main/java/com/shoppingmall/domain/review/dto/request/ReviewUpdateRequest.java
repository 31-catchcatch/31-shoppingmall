package com.shoppingmall.domain.review.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

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
    @Size(max = 2000, message = "리뷰는 2,000자 이하여야 합니다.")   // [1-6]
    @NoHtml                                                        // [1-1]
    private String content;

    @Size(max = 512, message = "이미지 URL은 512자 이하여야 합니다.")                    // [1-6]
    @Pattern(regexp = "^$|^(/uploads/|https?://)[\\w\\-./%]*$",                       // [1-1]
             message = "허용되지 않은 이미지 경로입니다.")
    private String imageUrl;
}
