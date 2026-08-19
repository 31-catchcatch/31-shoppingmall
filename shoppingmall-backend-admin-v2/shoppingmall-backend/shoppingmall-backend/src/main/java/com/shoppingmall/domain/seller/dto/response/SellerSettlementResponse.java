package com.shoppingmall.domain.seller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 판매자 정산 요약 응답 DTO
 */
public record SellerSettlementResponse(

        LocalDate startDate,
        LocalDate endDate,

        // 구매 확정된 주문의 총매출
        BigDecimal totalSalesAmount,

        // 플랫폼 수수료
        BigDecimal commissionAmount,

        // 판매자에게 지급될 정산 예정 금액
        BigDecimal expectedSettlementAmount,

        // 정산 완료 금액
        BigDecimal completedSettlementAmount

) {
}