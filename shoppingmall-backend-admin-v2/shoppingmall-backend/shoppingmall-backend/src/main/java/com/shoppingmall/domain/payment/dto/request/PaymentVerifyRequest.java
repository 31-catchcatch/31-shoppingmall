package com.shoppingmall.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentVerifyRequest {

    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;

    @NotBlank(message = "PG사 결제 거래식별번호(ID)는 필수입니다.")
    private String pgTransactionId; // 포트원의 imp_uid 혹은 토스의 paymentKey 등

    @NotBlank(message = "결제 수단은 필수입니다.")
    private String payMethod; // CARD, VBANK 등

    private String pgProvider; // KAKAO, TOSS 등

    @Min(value = 1, message = "검증할 결제 금액은 0보다 커야 합니다.")
    private int amount; // 프론트에서 실제 승인받은 가액
}