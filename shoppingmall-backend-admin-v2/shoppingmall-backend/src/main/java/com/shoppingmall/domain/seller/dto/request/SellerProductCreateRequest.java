package com.shoppingmall.domain.seller.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SellerProductCreateRequest(

        @NotNull(message = "카테고리를 선택해 주세요.")
        Long categoryId,

        @NotBlank(message = "상품명을 입력해 주세요.")
        @Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
        String productName,

        @NotNull(message = "상품 가격을 입력해 주세요.")
        @PositiveOrZero(message = "상품 가격은 0원 이상이어야 합니다.")
        Integer price,

        @NotNull(message = "할인율을 입력해 주세요.")
        @Min(value = 0, message = "할인율은 0 이상이어야 합니다.")
        @Max(value = 100, message = "할인율은 100 이하여야 합니다.")
        Integer discountRate,

        String description,

        @Size(max = 512, message = "썸네일 URL은 512자 이하여야 합니다.")
        String thumbnailUrl

) {
}