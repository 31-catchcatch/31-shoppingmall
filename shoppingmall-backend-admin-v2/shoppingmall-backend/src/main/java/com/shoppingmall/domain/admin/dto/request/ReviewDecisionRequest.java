package com.shoppingmall.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

/** 관리자 승인/반려 공용 요청 (입점 신청, 쿠폰 발행 요청 둘 다 이 형태) */
public record ReviewDecisionRequest(
        @NotNull Decision decision,
        String rejectionReason // decision=REJECT 일 때만 사용
) {
    public enum Decision { APPROVE, REJECT }
}
