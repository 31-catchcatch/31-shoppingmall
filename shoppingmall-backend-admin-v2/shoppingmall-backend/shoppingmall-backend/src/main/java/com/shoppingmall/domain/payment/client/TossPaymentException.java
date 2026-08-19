package com.shoppingmall.domain.payment.client;

import lombok.Getter;

/**
 * 토스 API 호출이 실패했을 때 클라이언트 계층이 던지는 예외.
 *
 * 이 프로젝트의 일반 예외 관행은 CustomException + ErrorCode 지만, 여기서는
 * "토스가 무엇 때문에 거절했는지"(code/message)를 그대로 들고 올라가야 한다.
 * 서비스 계층(PaymentService)이 이 예외를 받아 ErrorCode 로 번역하고 Payment 를 FAILED 로 기록한다.
 *
 * 두 가지 실패를 구분한다:
 *  - 응답을 받았지만 거절당함  -> httpStatus 존재, code 는 토스 에러코드 (예: NOT_FOUND_PAYMENT)
 *  - 응답 자체를 못 받음(타임아웃/DNS/방화벽) -> httpStatus 가 null, code 는 NETWORK_ERROR
 *
 * 후자는 배포 환경에서 was 의 외부 HTTPS egress 가 막혔을 때 나타나는 증상이고
 * 로컬에서는 재현되지 않으므로, 로그에서 구분이 되어야 원인을 빨리 찾을 수 있다.
 */
@Getter
public class TossPaymentException extends RuntimeException {

    /** 통신 자체가 실패했을 때 쓰는 의사(pseudo) 에러코드. 토스가 내려주는 코드가 아니다. */
    public static final String NETWORK_ERROR = "NETWORK_ERROR";

    private final String code;
    private final Integer httpStatus; // 통신 실패 시 null

    public TossPaymentException(String code, String message, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static TossPaymentException networkError(Throwable cause) {
        return new TossPaymentException(
                NETWORK_ERROR,
                "결제 서버와 통신하지 못했습니다: " + cause.getMessage(),
                null,
                cause);
    }

    /** 응답을 아예 받지 못한 통신 실패인지 여부 */
    public boolean isNetworkError() {
        return httpStatus == null;
    }
}
