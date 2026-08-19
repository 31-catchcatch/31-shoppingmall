package com.shoppingmall.domain.admin.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.NotNull;

/** 관리자 승인/반려 공용 요청 (입점 신청, 쿠폰 발행 요청 둘 다 이 형태) */
public record ReviewDecisionRequest(
        @NotNull Decision decision,
        // decision=REJECT 일 때만 사용
        @Size(max = 500, message = "반려 사유는 500자 이하여야 합니다.")   // [1-6]
        @NoHtml                                                          // [1-1]
        String rejectionReason
) {
    public enum Decision { APPROVE, REJECT }
}
