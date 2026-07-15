package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.response.AdminSettlementResponse;
import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.repository.OrderDetailRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * API 명세서 "관리자 - 정산 - 플랫폼 정산 관리 대시보드" (GET /admin/settlements).
 *
 * SellerSettlementService와 같은 방식(별도 Settlement 테이블 없이 구매확정 주문 데이터로 실시간 집계)을
 * 그대로 따르되, 판매자 한 명이 아니라 플랫폼 전체를 대상으로 계산한다.
 * 수수료율도 SellerSettlementService와 동일하게 10%로 맞춰뒀다 (나중에 설정값으로 빼도 됨).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSettlementService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.10");

    private final OrderDetailRepository orderDetailRepository;

    public AdminSettlementResponse getSettlements(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_PERIOD);
        }

        LocalDateTime startDateTime = resolvedStart.atStartOfDay();
        LocalDateTime endDateTime = resolvedEnd.plusDays(1).atStartOfDay();

        Long confirmedSalesValue = orderDetailRepository.sumTotalPriceByDeliveryStatusAndPeriod(
                DeliveryStatus.CONFIRMED, startDateTime, endDateTime);

        BigDecimal totalSalesAmount = BigDecimal.valueOf(confirmedSalesValue == null ? 0L : confirmedSalesValue);
        BigDecimal totalCommissionAmount = totalSalesAmount.multiply(COMMISSION_RATE).setScale(0, RoundingMode.DOWN);
        BigDecimal totalPayoutAmount = totalSalesAmount.subtract(totalCommissionAmount);

        return new AdminSettlementResponse(
                resolvedStart, resolvedEnd, totalSalesAmount, totalCommissionAmount, totalPayoutAmount);
    }
}
