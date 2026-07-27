package com.shoppingmall.domain.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 토스 API 오류 응답 본문. 4xx/5xx 일 때 { "code": "...", "message": "..." } 형태로 온다.
 * 예) NOT_FOUND_PAYMENT, ALREADY_PROCESSED_PAYMENT, REJECT_CARD_COMPANY,
 *     IDEMPOTENT_REQUEST_PROCESSING(409, 같은 멱등키 요청이 아직 처리 중)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossErrorResponse {

    private String code;
    private String message;
}
