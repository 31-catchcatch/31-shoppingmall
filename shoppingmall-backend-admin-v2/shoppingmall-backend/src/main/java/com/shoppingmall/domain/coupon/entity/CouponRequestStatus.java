package com.shoppingmall.domain.coupon.entity;

/**
 * 판매자 쿠폰 발행 요청의 처리 상태
 */
public enum CouponRequestStatus {

    PENDING,    // 관리자 승인 대기
    APPROVED,   // 관리자 승인 완료
    REJECTED,   // 관리자 반려
    CANCELED    // 판매자 요청 취소
}