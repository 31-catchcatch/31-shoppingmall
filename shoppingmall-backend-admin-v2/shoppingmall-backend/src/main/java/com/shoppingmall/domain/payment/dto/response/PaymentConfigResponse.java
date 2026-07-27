package com.shoppingmall.domain.payment.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * GET /api/v1/payments/config - 프론트가 토스 결제창을 띄우는 데 필요한 환경값.
 *
 * 프론트(frontdist)에 빌드 스텝이 없어 환경별로 상수를 갈아끼울 방법이 없으므로,
 * 로컬/VM/클라우드에서 달라지는 값을 서버가 런타임에 내려준다.
 * clientKey 는 결제창 호출에 쓰이는 공개값이라 비로그인 상태에서도 조회 가능하다.
 * (secretKey 는 서버 전용이므로 절대 이 응답에 포함하지 않는다.)
 */
@Getter
public class PaymentConfigResponse {

    private final String clientKey;
    private final String successUrl;
    private final String failUrl;

    @Builder
    public PaymentConfigResponse(String clientKey, String successUrl, String failUrl) {
        this.clientKey = clientKey;
        this.successUrl = successUrl;
        this.failUrl = failUrl;
    }
}
