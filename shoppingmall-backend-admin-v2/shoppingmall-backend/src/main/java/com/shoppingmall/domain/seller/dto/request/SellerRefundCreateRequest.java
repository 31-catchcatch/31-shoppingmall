package com.shoppingmall.domain.seller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 판매자 최종 환불 요청 DTO
 */
public record SellerRefundCreateRequest(

        @NotNull(message = "클레임 ID를 입력해 주세요.")
        Long claimId,

        @Size(
                max = 500,
                message = "환불 처리 메모는 500자 이하여야 합니다."
        )
        String memo

) {
}