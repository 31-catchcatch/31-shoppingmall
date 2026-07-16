package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.response.AdminSettlementResponse;
import com.shoppingmall.domain.settlement.entity.SettlementStatus;
import com.shoppingmall.domain.settlement.repository.SettlementRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * API 명세서 "관리자 - 정산 - 플랫폼 정산 관리 대시보드" (GET /admin/settlements).
 *
 * settlements 테이블(구매확정 시점에 OrderService가 자동 생성)을 플랫폼 전체 기준으로 집계한다.
 * PATCH /admin/settlements/{settlementId}/complete 로 개별 정산 건을 완료 처리할 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSettlementService {

    private final SettlementRepository settlementRepository;

    public AdminSettlementResponse getSettlements(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate resolvedEnd = endDate == null ? LocalDate.now() : endDate;

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_PERIOD);
        }

        LocalDateTime startDateTime = resolvedStart.atStartOfDay();
        LocalDateTime endDateTime = resolvedEnd.plusDays(1).atStartOfDay();

        Long totalSalesAmount = settlementRepository.sumSaleAmountAll(startDateTime, endDateTime);
        Long totalCommissionAmount = settlementRepository.sumFeeAmountAll(startDateTime, endDateTime);
        Long totalPayoutAmount = settlementRepository.sumSettlementAmountAllByStatus(
                SettlementStatus.PENDING, startDateTime, endDateTime)
                + settlementRepository.sumSettlementAmountAllByStatus(
                SettlementStatus.COMPLETED, startDateTime, endDateTime);

        return new AdminSettlementResponse(
                resolvedStart, resolvedEnd,
                BigDecimal.valueOf(totalSalesAmount),
                BigDecimal.valueOf(totalCommissionAmount),
                BigDecimal.valueOf(totalPayoutAmount));
    }

    /** PATCH /admin/settlements/{settlementId}/complete - 관리자가 실제 지급 처리 완료를 기록 */
    @Transactional
    public void completeSettlement(Long settlementId) {
        var settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND));
        settlement.complete();
    }
}
