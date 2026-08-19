package com.shoppingmall.domain.order.dto.response;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.payment.entity.Payment;

import java.time.LocalDateTime;

/** GET /api/v1/orders/{orderId}/receipt - PG 연동 전자 영수증 정보 */
public record OrderReceiptResponse(
        String orderNumber,
        String pgProvider,
        String payMethod,
        String pgTransactionId,
        int amount,
        String paymentStatus,
        LocalDateTime paidAt
) {
    public static OrderReceiptResponse from(Order order, Payment payment) {
        return new OrderReceiptResponse(
                order.getOrderNumber(),
                payment.getPgProvider(),
                payment.getPayMethod(),
                payment.getPgTransactionId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getCreatedAt()
        );
    }
}
