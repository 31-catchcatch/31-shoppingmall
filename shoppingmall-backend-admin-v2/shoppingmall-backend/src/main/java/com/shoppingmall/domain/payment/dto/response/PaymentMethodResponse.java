package com.shoppingmall.domain.payment.dto.response;

import com.shoppingmall.domain.payment.entity.PaymentMethod;

/** GET /api/v1/users/me/payments 목록의 개별 항목 - billingKey 원문은 절대 내려주지 않는다 */
public record PaymentMethodResponse(
        Long id,
        String pgProvider,
        String alias,
        String maskedCardNumber,
        boolean defaultMethod
) {
    public static PaymentMethodResponse from(PaymentMethod paymentMethod) {
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getPgProvider(),
                paymentMethod.getAlias(),
                paymentMethod.getMaskedCardNumber(),
                paymentMethod.isDefaultMethod()
        );
    }
}
