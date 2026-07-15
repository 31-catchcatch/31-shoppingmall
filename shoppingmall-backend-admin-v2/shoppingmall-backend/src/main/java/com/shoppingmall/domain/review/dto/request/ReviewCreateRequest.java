package com.shoppingmall.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewCreateRequest {

    @NotNull(message = "대상 상품 ID는 필수입니다.")
    private Long productId;

    @NotNull(message = "주문 세부 내역(OrderDetail) ID는 필수입니다.")
    private Long orderDetailId; // 구매 확정 유효성 검증용 식별값

    @Min(value = 1, message = "최소 별점은 1점입니다.")
    @Max(value = 5, message = "최대 별점은 5점입니다.")
    private int rating;

    @NotBlank(message = "리뷰 내용은 필수 입력 사항입니다.")
    private String content;

    private String imageUrl; // 포토 리뷰 이미지 (선택)
}