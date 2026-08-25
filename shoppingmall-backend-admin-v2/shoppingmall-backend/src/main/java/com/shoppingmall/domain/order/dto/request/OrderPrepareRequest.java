package com.shoppingmall.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 주문서 진입 요청. 바로구매와 장바구니 주문 두 경로를 모두 받는다.
 * 둘 중 하나만 채워 보낸다.
 */
public record OrderPrepareRequest(

        /** 바로구매 경로 */
        @Valid List<@Valid DirectItem> items,

        /** 장바구니 경로 — 선택한 장바구니 항목 id */
        @Size(max = 100, message = "한 번에 주문할 수 있는 항목 수를 초과했습니다.")
        List<Long> cartItemIds

) {
    public record DirectItem(

            @NotNull(message = "상품 ID가 필요합니다.")
            Long productId,

            @NotNull(message = "옵션 ID가 필요합니다.")
            Long optionId,

            @NotNull(message = "주문 수량이 필요합니다.")
            @Positive(message = "주문 수량은 1개 이상이어야 합니다.")
            @Max(value = 99, message = "1회 주문 수량은 99개 이하여야 합니다.")
            Integer quantity

    ) {}
}
