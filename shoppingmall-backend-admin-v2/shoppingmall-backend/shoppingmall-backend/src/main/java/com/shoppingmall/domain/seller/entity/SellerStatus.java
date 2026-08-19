package com.shoppingmall.domain.seller.entity;

public enum SellerStatus {

    ACTIVE,         // 정상 영업
    SUSPENDED,      // 일시 정지
    CLOSED,         // 자진 폐점
    FORCED_CLOSED   // 관리자 강제 폐점
}