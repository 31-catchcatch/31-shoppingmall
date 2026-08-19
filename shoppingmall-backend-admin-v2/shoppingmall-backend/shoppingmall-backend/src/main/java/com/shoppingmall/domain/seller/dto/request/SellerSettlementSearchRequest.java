package com.shoppingmall.domain.seller.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 판매자 정산 내역 조회 조건
 *
 * GET 요청의 Query Parameter로 사용한다.
 */
public record SellerSettlementSearchRequest(

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,

        String status,

        Integer page,

        Integer size

) {
}