package com.shoppingmall.domain.admin.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/** GET /api/v1/admin/settlements - 플랫폼 전체 거래액/수수료 수익 요약 */
public record AdminSettlementResponse(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalSalesAmount,      // 플랫폼 전체 거래액 (구매확정 기준)
        BigDecimal totalCommissionAmount, // 플랫폼 수수료 수익
        BigDecimal totalPayoutAmount      // 판매자들에게 지급될 총액 (거래액 - 수수료)
) {
}
