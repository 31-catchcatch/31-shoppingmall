package com.shoppingmall.domain.order.dto.request;

import com.shoppingmall.global.validation.NoHtml;

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
        @NoHtml                                                                       // [1-1]
        String receiverName,

        @NotBlank(message = "수령인 연락처를 입력해 주세요.")
        @Size(max = 20)                                                               // [1-6]
        @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$",                           // [1-6]
                 message = "연락처 형식이 올바르지 않습니다.")
        String receiverPhone,

        @NotBlank(message = "우편번호를 입력해 주세요.")
        @Pattern(regexp = "^\\d{5,6}$", message = "우편번호 형식이 올바르지 않습니다.")   // [1-6]
        String zipCode,

        @NotBlank(message = "주소를 입력해 주세요.")
        @Size(max = 255)                                                              // [1-6]
        @NoHtml                                                                       // [1-1]
        String address,

        @Size(max = 255)                                                              // [1-6]
        @NoHtml                                                                       // [1-1]
        String addressDetail,

        @Size(max = 255)
        @NoHtml                                                                       // [1-1]
        String deliveryRequest

) {

    public record OrderItemRequest(

            @NotNull(message = "상품 ID가 필요합니다.")
            Long productId,

            // [1-3 조치] optionId 를 생략하면 옵션 검증과 재고 차감을 통째로 건너뛰게 되므로 필수로 강제한다.
            //            상품 등록 시 옵션이 최소 1개 강제(@NotEmpty)이므로 null 은 정상 경로가 아니다.
            @NotNull(message = "옵션 ID가 필요합니다.")
            Long optionId,

            @NotNull(message = "주문 수량이 필요합니다.")
            @Positive(message = "주문 수량은 1개 이상이어야 합니다.")
            // [1-3][1-6 조치] 상한이 없으면 unitPrice * quantity 에서 int 오버플로가 발생할 수 있다.
            @Max(value = 99, message = "1회 주문 수량은 99개 이하여야 합니다.")
            Integer quantity

    ) {
    }
}