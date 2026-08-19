package com.shoppingmall.domain.seller.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 판매자 매출 통계 조회 조건
 *
 * 요청 예시:
 * GET /api/v1/seller/sales
 * GET /api/v1/seller/sales?startDate=2026-07-01&endDate=2026-07-31
 */
public record SellerSalesSearchRequest(

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate

) {
}