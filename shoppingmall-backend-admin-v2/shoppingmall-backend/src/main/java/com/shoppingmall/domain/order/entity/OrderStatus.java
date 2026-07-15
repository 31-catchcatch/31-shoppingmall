package com.shoppingmall.domain.order.entity;

/**
 * 전체 주문의 진행 상태
 */
public enum OrderStatus {

    PENDING,          // 주문 생성, 결제 전
    PAID,             // 결제 완료
    PARTIALLY_SHIPPED,// 일부 상품 배송 시작
    SHIPPED,          // 전체 상품 배송 시작
    COMPLETED,        // 전체 구매 확정
    CANCELED,         // 주문 취소
    REFUNDED          // 전체 환불
}