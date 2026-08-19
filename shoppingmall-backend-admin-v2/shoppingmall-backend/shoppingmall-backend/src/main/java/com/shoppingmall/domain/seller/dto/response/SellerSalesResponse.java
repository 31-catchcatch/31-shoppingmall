package com.shoppingmall.domain.seller.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * 판매자 기간별 매출 통계 응답
 */
public record SellerSalesResponse(

        LocalDate startDate,
        LocalDate endDate,

        // 기간 내 총매출
        Long totalSalesAmount,

        // 주문 상품 건수
        Long orderCount,

        // 전체 판매 수량
        Long totalSoldQuantity,

        // 상품별 판매 통계
        List<SellerProductSalesResponse> productSales

) {
}