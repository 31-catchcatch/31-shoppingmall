package com.shoppingmall.domain.product.entity;

/**
 * 상품 판매 상태.
 *
 * is_deleted(삭제)와는 독립적인 축이다.
 *  - ON_SALE   : 판매중 (기본값). 손님 목록/검색/상세에 노출된다.
 *  - SUSPENDED : 판매자가 일시적으로 내린 상태. 판매자 본인 목록에는 보이지만
 *                손님에게는 목록/검색/상세에서 모두 감춰지고 주문도 차단된다.
 *
 * NOTE: 현재는 판매자 자율 판매중지만 지원한다. 추후 관리자 강제 판매중지가 필요해지면
 *       SUSPENDED_BY_ADMIN 같은 값을 추가해 "누가 내렸는가"를 구분한다
 *       (판매자가 관리자 제재를 임의로 재개하지 못하게 하기 위함).
 */
public enum ProductStatus {

    ON_SALE,
    SUSPENDED
}
