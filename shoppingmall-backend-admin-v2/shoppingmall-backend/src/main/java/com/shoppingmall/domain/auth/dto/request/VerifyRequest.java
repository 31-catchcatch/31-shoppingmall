package com.shoppingmall.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 본인인증 요청 (mock). 실제 연동 시 SMS/PASS 사에서 요구하는 필드로 교체 필요. */
public record VerifyRequest(
        @NotBlank String name,
        @NotBlank String phoneNumber
) {
}
