package com.shoppingmall.domain.product.dto.response;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductOption;

import java.util.List;

/** GET /api/v1/products/{productId} - 상세 정보 + 사이즈/수량 옵션 조회 */
public record ProductDetailResponse(
        Long productId,
        String sellerName,
        String categoryName,
        String name,
        int price,
        int discountRate,
        int finalPrice,
        String description,
        List<String> imageUrls,
        List<OptionResponse> options
) {
    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getSeller().getBusinessName(),
                product.getCategory().getName(),
                product.getName(),
                product.getPrice(),
                product.getDiscountRate(),
                product.getFinalPrice(),
                product.getDescription(),
                product.getImages().stream().map(img -> img.getImageUrl()).toList(),
                product.getOptions().stream().map(OptionResponse::from).toList()
        );
    }

    public record OptionResponse(
            Long optionId,
            String optionName,
            int additionalPrice,
            int stockQuantity,
            boolean soldOut
    ) {
        public static OptionResponse from(ProductOption option) {
            return new OptionResponse(
                    option.getId(),
                    option.getOptionName(),
                    option.getAdditionalPrice(),
                    option.getStockQuantity(),
                    option.isSoldOut()
            );
        }
    }
}
