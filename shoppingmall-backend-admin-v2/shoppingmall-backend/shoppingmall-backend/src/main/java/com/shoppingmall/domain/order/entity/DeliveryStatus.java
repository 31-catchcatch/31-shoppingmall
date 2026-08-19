package com.shoppingmall.domain.order.entity;

/**
 * 주문 상품별 배송 및 구매확정 상태
 */
public enum DeliveryStatus {

    PAYMENT_COMPLETED, // 결제 완료
    PREPARING,         // 상품 준비 중
    SHIPPING,          // 배송 중
    DELIVERED,         // 배송 완료
    CONFIRMED,         // 구매 확정
    CANCELED,          // 주문 상품 취소
    RETURN_REQUESTED,  // 반품 신청
    EXCHANGE_REQUESTED,// 교환 신청
    REFUNDED           // 환불 완료
}