package com.shoppingmall.domain.payment.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingmall.domain.payment.client.dto.TossConfirmResponse;
import com.shoppingmall.domain.payment.client.dto.TossErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 결제 API 클라이언트.
 *
 * 인증: Authorization: Basic base64(secretKey + ":")  <- 시크릿 키 뒤의 콜론이 반드시 있어야 한다.
 *      (토스는 secretKey 를 사용자명으로, 비밀번호를 빈 문자열로 쓰는 Basic 인증을 사용한다)
 *
 * 멱등키: Idempotency-Key 헤더. 같은 키로 재요청하면 토스가 첫 응답을 그대로 돌려주므로
 *      네트워크 오류로 재시도해도 이중 승인이 나지 않는다. 15일간 유효, 최대 300자.
 *      키를 매번 UUID 로 새로 만들면 재시도가 별개 요청이 되어 멱등성이 의미가 없어지므로,
 *      주문 단위로 항상 같은 값이 나오도록 orderId(= Order.orderNumber)에서 파생시킨다.
 */
@Slf4j
@Component
public class TossPaymentClient {

    private static final String CONFIRM_PATH = "/v1/payments/confirm";
    private static final String CANCEL_PATH  = "/v1/payments/{paymentKey}/cancel";
    private final RestClient restClient;
    private final String authorizationHeader;
    private final ObjectMapper objectMapper;

    public TossPaymentClient(RestClient.Builder restClientBuilder,
                             ObjectMapper objectMapper,
                             @Value("${payment.toss.base-url:https://api.tosspayments.com}") String baseUrl,
                             @Value("${payment.toss.secret-key:}") String secretKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.authorizationHeader = buildBasicAuthHeader(secretKey);
    }

    private static String buildBasicAuthHeader(String secretKey) {
        // 콜론 필수. UTF-8 로 인코딩하며 BOM 이 붙으면 안 된다.
        String raw = secretKey + ":";
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 결제 승인. 프론트가 결제창에서 받아온 paymentKey 를 서버가 토스에 직접 확인시킨다.
     *
     * @param paymentKey 토스가 발급한 결제 키 (successUrl 쿼리로 전달됨)
     * @param orderId    Order.orderNumber (토스 제약: 6~64자. DB PK 숫자는 짧아서 쓸 수 없다)
     * @param amount     서버가 확정한 결제 금액
     * @throws TossPaymentException 토스가 거절했거나 통신 자체가 실패한 경우
     */
    public TossConfirmResponse confirm(String paymentKey, String orderId, int amount) {
        try {
            return restClient.post()
                    .uri(CONFIRM_PATH)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .header("Idempotency-Key", idempotencyKey(orderId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderId,
                            "amount", amount))
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw toException(response.getStatusCode().value(), readBody(response.getBody()));
                    })
                    .body(TossConfirmResponse.class);

        } catch (ResourceAccessException e) {
            // 타임아웃 / DNS 실패 / 방화벽 차단 등 응답 자체를 못 받은 경우.
            // 배포 환경에서 was 의 아웃바운드 443 이 막히면 여기로 떨어진다.
            log.error("[TOSS] 결제 승인 통신 실패. orderId={}", orderId, e);
            throw TossPaymentException.networkError(e);
        }
    }
    /**
     * 승인된 결제를 전액 취소한다. 금액 불일치를 감지했을 때 호출한다.
     *
     * <p>멱등키를 paymentKey 에서 파생시켜, 재시도해도 이중 취소 요청이 되지 않게 한다.
     *
     * @throws TossPaymentException 토스가 거절했거나 통신 자체가 실패한 경우
     */
    public void cancel(String paymentKey, String cancelReason) {
        try {
            restClient.post()
                    .uri(CANCEL_PATH, paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .header("Idempotency-Key", "cancel-" + paymentKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("cancelReason", cancelReason))
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw toException(response.getStatusCode().value(), readBody(response.getBody()));
                    })
                    .toBodilessEntity();
  
        } catch (ResourceAccessException e) {
            log.error("[TOSS] 결제 취소 통신 실패. paymentKey={}", paymentKey, e);
            throw TossPaymentException.networkError(e);
        }
    }
    /** 같은 주문의 재시도가 항상 같은 키를 쓰도록 orderId 에서 파생시킨다 (최대 300자 제한 내). */
    private String idempotencyKey(String orderId) {
        return "confirm-" + orderId;
    }

    private String readBody(java.io.InputStream body) {
        try {
            return StreamUtils.copyToString(body, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private TossPaymentException toException(int httpStatus, String rawBody) {
        String code = "UNKNOWN";
        String message = "결제 승인에 실패했습니다.";

        try {
            TossErrorResponse error = objectMapper.readValue(rawBody, TossErrorResponse.class);
            if (error.getCode() != null) code = error.getCode();
            if (error.getMessage() != null) message = error.getMessage();
        } catch (Exception e) {
            // 토스가 JSON 이 아닌 응답(게이트웨이 오류 페이지 등)을 준 경우 원문 일부를 남긴다.
            log.warn("[TOSS] 오류 응답 파싱 실패. status={}, body={}", httpStatus, abbreviate(rawBody));
        }

        log.warn("[TOSS] 결제 승인 거절. status={}, code={}, message={}", httpStatus, code, message);
        return new TossPaymentException(code, message, httpStatus, null);
    }

    private String abbreviate(String value) {
        if (value == null) return "";
        return value.length() <= 300 ? value : value.substring(0, 300) + "...(생략)";
    }
}
