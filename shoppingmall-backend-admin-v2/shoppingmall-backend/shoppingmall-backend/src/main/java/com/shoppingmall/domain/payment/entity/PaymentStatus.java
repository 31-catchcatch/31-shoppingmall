package com.shoppingmall.domain.payment.entity;

public enum PaymentStatus {
    READY,      // 결제 대기
    PAID,       // 결제 완료 (승인 완료)
    FAILED,     // 결제 실패
    CANCELLED   // 결제 취소 (환불 완료)
}