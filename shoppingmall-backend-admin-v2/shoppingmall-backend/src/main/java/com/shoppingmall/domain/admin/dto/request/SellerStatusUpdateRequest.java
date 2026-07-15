package com.shoppingmall.domain.admin.dto.request;

import com.shoppingmall.domain.seller.entity.SellerStatus;
import jakarta.validation.constraints.NotNull;

public record SellerStatusUpdateRequest(
        @NotNull SellerStatus status
) {
}
