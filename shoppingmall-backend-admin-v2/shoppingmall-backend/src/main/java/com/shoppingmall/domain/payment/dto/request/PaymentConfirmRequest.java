package com.shoppingmall.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토스 결제 승인 요청(POST /api/v1/payments/confirm).
 *
 * 토스가 successUrl 로 리다이렉트하면서 쿼리스트링으로 넘겨준 세 값을 프론트가 그대로 전달한다.
 *
 * ⚠️ orderId 는 DB PK(Long)가 아니라 Order.orderNumber(String)다.
 *    토스의 orderId 는 6~64자 제약이 있어 1~2자리 숫자 PK 를 쓸 수 없기 때문이다.
 *    기존 PaymentVerifyRequest.orderId 는 Long 이므로 혼동하지 말 것.
 *
 * amount 는 여기서 받되 신뢰하지 않는다. 서버가 Order.finalPaymentAmount 와 대조해
 * 다르면 토스를 호출하기도 전에 차단한다.
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    @NotBlank(message = "결제 키는 필수입니다.")
    private String paymentKey;

    @NotBlank(message = "주문번호는 필수입니다.")
    private String orderId; // = Order.orderNumber

    @Min(value = 1, message = "결제 금액은 0보다 커야 합니다.")
    private int amount;
}
