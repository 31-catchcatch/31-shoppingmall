package com.shoppingmall.domain.seller.dto.request;

import com.shoppingmall.domain.product.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

/**
 * PATCH /api/v1/seller/products/{productId}/status
 * 판매자가 상품을 판매중지(SUSPENDED) 하거나 다시 판매중(ON_SALE)으로 되돌린다.
 */
public record SellerProductStatusUpdateRequest(

        @NotNull(message = "변경할 판매 상태를 지정해 주세요.")
        ProductStatus status

) {
}
