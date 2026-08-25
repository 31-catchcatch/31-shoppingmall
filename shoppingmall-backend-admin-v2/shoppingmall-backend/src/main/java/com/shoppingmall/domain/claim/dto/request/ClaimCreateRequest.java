package com.shoppingmall.domain.claim.dto.request;

import com.shoppingmall.global.validation.NoHtml;

import com.shoppingmall.domain.claim.entity.ClaimType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 사용자의 교환·환불 신청 요청 DTO
 */
public record ClaimCreateRequest(

        /**
         * 클레임 대상 주문 상세 ID
         */
        @NotNull(message = "주문 상세 ID가 필요합니다.")
        Long orderDetailId,

        /**
         * RETURN 또는 EXCHANGE
         */
        @NotNull(message = "클레임 유형을 선택해 주세요.")
        ClaimType type,

        /**
         * 교환·환불 신청 사유
         */
        @NotBlank(message = "클레임 신청 사유를 입력해 주세요.")
        @Size(
                max = 1000,
                message = "신청 사유는 1,000자 이하여야 합니다."
        )
        @NoHtml   // [1-1]
        String reason

) {
}