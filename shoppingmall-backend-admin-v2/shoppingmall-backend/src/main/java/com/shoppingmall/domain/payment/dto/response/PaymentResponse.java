package com.shoppingmall.domain.payment.dto.response;

import com.shoppingmall.domain.payment.entity.Payment;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class PaymentResponse {

    private final Long paymentId;
    private final Long orderId;
    private final String pgTransactionId;
    private final int amount;
    private final String payMethod; // 결제 완료 화면에 노출. 토스 경로에서는 토스가 내려준 값이 그대로 들어온다
    private final String status;
    private final LocalDateTime paymentDate;

    @Builder
    public PaymentResponse(Long paymentId, Long orderId, String pgTransactionId,
                           int amount, String payMethod, String status, LocalDateTime paymentDate) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.pgTransactionId = pgTransactionId;
        this.amount = amount;
        this.payMethod = payMethod;
        this.status = status;
        this.paymentDate = paymentDate;
    }

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .pgTransactionId(payment.getPgTransactionId())
                .amount(payment.getAmount())
                .payMethod(payment.getPayMethod())
                .status(payment.getStatus().name())
                .paymentDate(payment.getCreatedAt())
                .build();
    }
}