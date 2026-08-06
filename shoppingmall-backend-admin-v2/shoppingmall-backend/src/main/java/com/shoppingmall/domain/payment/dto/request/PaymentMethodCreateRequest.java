package com.shoppingmall.domain.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** POST /api/v1/users/me/payments - 새 간편결제 수단(빌링키) 등록 요청 */
@Getter
@NoArgsConstructor
public class PaymentMethodCreateRequest {

    @NotBlank(message = "PG사 정보는 필수입니다.")
    private String pgProvider;

    @NotBlank(message = "빌링키는 필수입니다.")
    private String billingKey;

    private String alias;

    private String maskedCardNumber;

    private boolean defaultMethod;
}
