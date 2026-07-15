package com.shoppingmall.domain.seller.dto.response;

import java.math.BigDecimal;

/**
 * 판매자 대시보드 요약 응답 DTO
 */
public record SellerDashboardResponse(

        BigDecimal todaySales,
        long newOrderCount,
        long unansweredQnaCount,
        long pendingClaimCount

) {
}