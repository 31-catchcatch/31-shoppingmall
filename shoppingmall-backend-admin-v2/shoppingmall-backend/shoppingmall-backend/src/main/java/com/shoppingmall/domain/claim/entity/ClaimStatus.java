package com.shoppingmall.domain.claim.entity;

/**
 * 교환/환불 진행 상태
 */
public enum ClaimStatus {

    REQUESTED,     // 신청
    ACCEPTED,      // 접수
    REJECTED,      // 반려
    PROCESSING,    // 처리중
    COMPLETED      // 완료

}