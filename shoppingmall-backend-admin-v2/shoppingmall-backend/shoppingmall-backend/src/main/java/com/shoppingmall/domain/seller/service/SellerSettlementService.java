package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.seller.dto.request.SellerSettlementSearchRequest;
import com.shoppingmall.domain.seller.dto.response.SellerSettlementResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
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
 * 판매자 정산 내역 조회 서비스
 *
 * 담당 API
 * GET /api/v1/seller/settlements
 *
 * settlements 테이블(구매확정 시점에 OrderService가 자동 생성)을 그대로 집계한다.
 * 예전에는 이 테이블이 없어 "완료 정산 금액"이 항상 0으로 고정되어 있었는데,
 * 이제 실제 COMPLETED 상태 합계를 조회한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerSettlementService {

    private final SellerApplicationRepository sellerApplicationRepository;
    private final SettlementRepository settlementRepository;

    public SellerSettlementResponse getSettlements(Long userId, SellerSettlementSearchRequest request) {
        SellerApplication seller = getApprovedSellerApplication(userId);

        LocalDate startDate = request.startDate() == null ? LocalDate.now().withDayOfMonth(1) : request.startDate();
        LocalDate endDate = request.endDate() == null ? LocalDate.now() : request.endDate();

        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_PERIOD);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Long totalSalesAmount = settlementRepository.sumSaleAmountBySeller(seller.getId(), startDateTime, endDateTime);
        Long commissionAmount = settlementRepository.sumFeeAmountBySeller(seller.getId(), startDateTime, endDateTime);
        Long expectedSettlementAmount = settlementRepository.sumSettlementAmountBySellerAndStatus(
                seller.getId(), SettlementStatus.PENDING, startDateTime, endDateTime);
        Long completedSettlementAmount = settlementRepository.sumSettlementAmountBySellerAndStatus(
                seller.getId(), SettlementStatus.COMPLETED, startDateTime, endDateTime);

        return new SellerSettlementResponse(
                startDate,
                endDate,
                BigDecimal.valueOf(totalSalesAmount),
                BigDecimal.valueOf(commissionAmount),
                BigDecimal.valueOf(expectedSettlementAmount),
                BigDecimal.valueOf(completedSettlementAmount)
        );
    }

    private SellerApplication getApprovedSellerApplication(Long userId) {
        SellerApplication application = sellerApplicationRepository.findFirstByUser_IdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLER_NOT_APPROVED));

        if (application.getStatus() != SellerApplicationStatus.APPROVED) {
            throw new CustomException(ErrorCode.SELLER_NOT_APPROVED);
        }

        return application;
    }
}