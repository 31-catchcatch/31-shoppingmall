package com.shoppingmall.domain.claim.dto.request;

import com.shoppingmall.global.validation.NoHtml;

import com.shoppingmall.domain.claim.entity.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 클레임 상태 변경 요청 DTO
 */
public record ClaimStatusUpdateRequest(

        @NotNull(message = "변경할 클레임 상태를 입력해 주세요.")
        ClaimStatus status,

        @Size(
                max = 1000,
                message = "처리 사유는 1,000자 이하여야 합니다."
        )
        @NoHtml   // [1-1]
        String processReason

) {
}