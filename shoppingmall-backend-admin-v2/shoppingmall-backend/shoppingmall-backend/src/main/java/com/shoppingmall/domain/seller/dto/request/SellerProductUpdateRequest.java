package com.shoppingmall.domain.seller.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Pattern;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SellerProductUpdateRequest(

        @NotNull(message = "카테고리를 선택해 주세요.")
        Long categoryId,

        @NotNull(message = "브랜드를 선택해 주세요.")
        Long brandId,

        @NotBlank(message = "상품명을 입력해 주세요.")
        @Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
        @NoHtml(message = "상품명에는 HTML 태그나 스크립트를 사용할 수 없습니다.")   // [1-1]
        String productName,

        @NotNull(message = "상품 가격을 입력해 주세요.")
        @PositiveOrZero(message = "상품 가격은 0원 이상이어야 합니다.")
        Integer price,

        @NotNull(message = "할인율을 입력해 주세요.")
        @Min(value = 0, message = "할인율은 0 이상이어야 합니다.")
        @Max(value = 100, message = "할인율은 100 이하여야 합니다.")
        Integer discountRate,

        // [1-6] 길이 제한 부재 보완. 태그 허용 필드이므로 @NoHtml 대신
        //       SellerProductService 에서 HtmlSanitizer 로 정제한다. [1-1]
        @Size(max = 10000, message = "상품 설명은 10,000자 이하여야 합니다.")
        String description,

        @Size(max = 512, message = "썸네일 URL은 512자 이하여야 합니다.")
        @Pattern(regexp = "^$|^(/uploads/|https?://)[\\w\\-./%]*$",            // [1-1][1-6]
                 message = "허용되지 않은 이미지 경로입니다.")
        String thumbnailUrl,

        @NotEmpty(message = "최소 1개 이상의 옵션을 등록해 주세요.")
        @Valid
        List<ProductOptionRequest> options,

        @NotEmpty(message = "최소 1장 이상의 상품 이미지를 등록해 주세요.")
        List<String> imageUrls

) {
}