package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.response.AdminSellerSettlementResponse;
import com.shoppingmall.domain.admin.dto.response.AdminSettlementResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.domain.settlement.entity.Settlement;
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
import java.util.List;

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
    private final SellerApplicationRepository sellerApplicationRepository;

    public AdminSettlementResponse getSettlements(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = resolveStart(startDate);
        LocalDate resolvedEnd = resolveEnd(endDate);
        validatePeriod(resolvedStart, resolvedEnd);

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

    /** GET /admin/settlements/sellers - 판매자별 정산 명세 (기간 집계) */
    public List<AdminSellerSettlementResponse> getSellerSettlements(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = resolveStart(startDate);
        LocalDate resolvedEnd = resolveEnd(endDate);
        validatePeriod(resolvedStart, resolvedEnd);

        LocalDateTime startDateTime = resolvedStart.atStartOfDay();
        LocalDateTime endDateTime = resolvedEnd.plusDays(1).atStartOfDay();

        List<SellerApplication> sellers = sellerApplicationRepository
                .findAllByStatusOrderByCreatedAtAsc(SellerApplicationStatus.APPROVED);

        return sellers.stream()
                .map(seller -> new AdminSellerSettlementResponse(
                        seller.getId(),
                        seller.getBusinessName(),
                        resolvedStart,
                        resolvedEnd,
                        settlementRepository.sumSaleAmountBySeller(seller.getId(), startDateTime, endDateTime),
                        settlementRepository.sumFeeAmountBySeller(seller.getId(), startDateTime, endDateTime),
                        settlementRepository.sumSettlementAmountBySellerAndStatus(
                                seller.getId(), SettlementStatus.PENDING, startDateTime, endDateTime),
                        settlementRepository.sumSettlementAmountBySellerAndStatus(
                                seller.getId(), SettlementStatus.COMPLETED, startDateTime, endDateTime)
                ))
                .filter(response -> response.saleAmount() > 0)
                .toList();
    }

    /** PATCH /admin/settlements/sellers/{sellerId}/complete - 판매자의 해당 기간 대기 정산 건 일괄 지급완료 처리 */
    @Transactional
    public void completeSellerSettlements(Long sellerId, LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStart = resolveStart(startDate);
        LocalDate resolvedEnd = resolveEnd(endDate);
        validatePeriod(resolvedStart, resolvedEnd);

        LocalDateTime startDateTime = resolvedStart.atStartOfDay();
        LocalDateTime endDateTime = resolvedEnd.plusDays(1).atStartOfDay();

        List<Settlement> pending = settlementRepository.findAllBySeller_IdAndStatusAndCreatedAtBetween(
                sellerId, SettlementStatus.PENDING, startDateTime, endDateTime);

        pending.forEach(Settlement::complete);
    }

    private LocalDate resolveStart(LocalDate startDate) {
        return startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
    }

    private LocalDate resolveEnd(LocalDate endDate) {
        return endDate == null ? LocalDate.now() : endDate;
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_PERIOD);
        }
    }
}
