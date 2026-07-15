package com.shoppingmall.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * 최종 주문 생성 요청
 */
public record OrderCreateRequest(

        @NotEmpty(message = "주문 상품이 필요합니다.")
        List<@Valid OrderItemRequest> items,

        Long couponId,

        @PositiveOrZero(message = "사용 포인트는 0 이상이어야 합니다.")
        Integer usePoint,

        @NotBlank(message = "수령인 이름을 입력해 주세요.")
        @Size(max = 50)
        String receiverName,

        @NotBlank(message = "수령인 연락처를 입력해 주세요.")
        String receiverPhone,

        @NotBlank(message = "우편번호를 입력해 주세요.")
        String zipCode,

        @NotBlank(message = "주소를 입력해 주세요.")
        String address,

        String addressDetail,

        @Size(max = 255)
        String deliveryRequest

) {

    public record OrderItemRequest(

            @NotNull(message = "상품 ID가 필요합니다.")
            Long productId,

            Long optionId,

            @NotNull(message = "주문 수량이 필요합니다.")
            @Positive(message = "주문 수량은 1개 이상이어야 합니다.")
            Integer quantity

    ) {
    }
}