package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductOption;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 판매자 상품 단건 응답 DTO
 */
public record SellerProductResponse(

        Long productId,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String productName,
        Integer price,
        Integer discountRate,
        Integer finalPrice,
        String description,
        String thumbnailUrl,
        Integer totalStock,
        boolean soldOut,
        String status,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    /**
     * Product 엔티티를 판매자 상품 응답 DTO로 변환한다.
     */
    public static SellerProductResponse from(Product product) {
        // 재고는 옵션 단위로 관리된다. 삭제되지 않은 옵션들의 재고 합을 totalStock 으로,
        // 살아있는 옵션이 있으면서 재고 합이 0인 경우만 품절(soldOut)로 본다.
        // (옵션이 아예 없는 상품은 재고 개념이 없으므로 soldOut=false, totalStock=0)
        List<ProductOption> activeOptions = product.getOptions().stream()
                .filter(option -> !option.isDeleted())
                .toList();
        int totalStock = activeOptions.stream()
                .mapToInt(ProductOption::getStockQuantity)
                .sum();
        boolean soldOut = !activeOptions.isEmpty() && totalStock == 0;

        return new SellerProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBrand() == null ? null : product.getBrand().getId(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                product.getName(),
                product.getPrice(),
                product.getDiscountRate(),
                product.getFinalPrice(),
                product.getDescription(),
                product.getThumbnailUrl(),
                totalStock,
                soldOut,
                product.getStatus().name(),
                product.isDeleted(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}