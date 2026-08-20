package com.shoppingmall.domain.order.dto.response;

import java.util.List;

/** 주문서 화면 표시용. 금액은 전부 서버가 확정한 값이다. */
public record OrderDraftResponse(
        String draftId,
        List<Item> items,
        int totalProductAmount,
        int shippingFee,
        int estimatedPaymentAmount
) {
    public record Item(
            Long productId,
            Long optionId,
            String productName,
            String optionName,
            String thumbnailUrl,
            int unitPrice,
            int quantity,
            int lineAmount
    ) {}
}
