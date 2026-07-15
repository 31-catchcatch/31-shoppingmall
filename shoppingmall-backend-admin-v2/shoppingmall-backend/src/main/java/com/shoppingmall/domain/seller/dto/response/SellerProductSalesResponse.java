package com.shoppingmall.domain.seller.dto.response;

/**
 * 판매자 상품별 매출 통계 응답
 */
public record SellerProductSalesResponse(

        Long productId,
        String productName,

        // 주문 상세 건수
        Long orderCount,

        // 총 판매 수량
        Long soldQuantity,

        // 상품별 총매출
        Long salesAmount

) {
}