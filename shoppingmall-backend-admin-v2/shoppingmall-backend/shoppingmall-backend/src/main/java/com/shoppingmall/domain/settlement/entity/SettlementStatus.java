package com.shoppingmall.domain.settlement.entity;

/**
 * 정산 상태.
 *
 * - PENDING   : 구매확정으로 정산 대상이 되었으나 아직 판매자에게 지급 전
 * - COMPLETED : 관리자가 실제 지급 완료 처리(PATCH /admin/settlements/{id}/complete)
 */
public enum SettlementStatus {
    PENDING,
    COMPLETED
}
