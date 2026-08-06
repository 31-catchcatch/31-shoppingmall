package com.shoppingmall.domain.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 토스 결제 승인 응답(POST /v1/payments/confirm) 중 우리가 쓰는 필드만 매핑한다.
 *
 * 토스 응답에는 카드/가상계좌/영수증 등 수십 개 필드가 들어 있으나 전부 매핑하지 않는다.
 * 토스가 필드를 추가해도 깨지지 않도록 ignoreUnknown = true 를 반드시 유지할 것.
 *
 * approvedAt 은 "2026-07-27T10:55:56+09:00" 형태의 문자열로 그대로 둔다.
 * 승인 시각은 Payment.createdAt 으로 남고, 이 값은 확인용이라 날짜 타입 변환이 필요 없다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossConfirmResponse {

    private String paymentKey;
    private String orderId;      // 우리가 보낸 Order.orderNumber 가 그대로 돌아온다
    private String status;       // DONE / CANCELED / ABORTED 등. 승인 성공은 DONE
    private Integer totalAmount; // 실제 승인된 금액. Order.finalPaymentAmount 와 대조해야 한다
    private String method;       // "카드", "가상계좌" 등 한글 결제수단명
    private String approvedAt;
}
