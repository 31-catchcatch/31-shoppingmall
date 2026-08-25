package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductOption;

import java.util.List;

/**
 * GET /api/v1/seller/products/{productId} - 판매자 상품 수정 화면 초기값 조회.
 * 공개 상품 상세 API(/products/{id})는 categoryId가 없어 수정 폼의 카테고리 select를
 * 채울 수 없었기 때문에 신설했다.
 */
public record SellerProductDetailResponse(

        Long productId,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String productName,
        Integer price,
        Integer discountRate,
        String description,
        String thumbnailUrl,
        List<String> imageUrls,
        List<OptionResponse> options

) {

    public static SellerProductDetailResponse from(Product product) {
        return new SellerProductDetailResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand() == null ? null : product.getBrand().getId(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                product.getName(),
                product.getPrice(),
                product.getDiscountRate(),
                product.getDescription(),
                product.getThumbnailUrl(),
                product.getImages().stream().map(image -> image.getImageUrl()).toList(),
                product.getActiveOptions().stream().map(OptionResponse::from).toList()
        );
    }

    public record OptionResponse(
            Long optionId,
            String optionName,
            int additionalPrice,
            int stockQuantity
    ) {
        public static OptionResponse from(ProductOption option) {
            return new OptionResponse(
                    option.getId(),
                    option.getOptionName(),
                    option.getAdditionalPrice(),
                    option.getStockQuantity()
            );
        }
    }
}
