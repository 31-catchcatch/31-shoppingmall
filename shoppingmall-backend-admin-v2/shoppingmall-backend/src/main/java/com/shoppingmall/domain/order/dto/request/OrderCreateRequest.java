package com.shoppingmall.domain.order.dto.request;

import com.shoppingmall.global.validation.NoHtml;

import jakarta.validation.constraints.*;

/**
 * 최종 주문 생성 요청.
 *
 * [1-3 조치] 주문 대상(productId·optionId·quantity)은 더 이상 받지 않는다.
 *            주문서 진입 시 서버가 확정해 보관한 draftId 만 받으므로 변조할 값이 없다.
 */
public record OrderCreateRequest(

        @NotBlank(message = "주문 정보가 필요합니다.")
        @Size(max = 64)
        String draftId,

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
}
