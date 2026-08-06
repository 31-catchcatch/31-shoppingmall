package com.shoppingmall.domain.admin.dto.response;

import java.time.LocalDate;

/** GET /api/v1/admin/settlements/sellers - 판매자별 정산 명세 (기간 집계) */
public record AdminSellerSettlementResponse(
        Long sellerId,
        String businessName,
        LocalDate startDate,
        LocalDate endDate,
        long saleAmount,
        long feeAmount,
        long pendingAmount,
        long completedAmount
) {
}
