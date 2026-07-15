package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.repository.OrderDetailRepository;
import com.shoppingmall.domain.seller.dto.request.SellerSettlementSearchRequest;
import com.shoppingmall.domain.seller.dto.response.SellerSettlementResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
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
 * 판매자 정산 내역 조회 서비스
 *
 * 담당 API
 * GET /api/v1/seller/settlements
 *
 * 현재는 별도의 Settlement 테이블이 없으므로,
 * 구매 확정된 주문 상품의 총액을 기준으로
 * 예상 정산 금액을 계산한다.
 *
 * 추후 정산 도메인이 병합되면
 * SettlementRepository 기반 조회 방식으로 변경한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerSettlementService {

    /**
     * 기본 플랫폼 수수료율: 10%
     *
     * 추후 판매자 등급이나 계약에 따라 달라진다면
     * DB 또는 설정 파일에서 가져오도록 변경한다.
     */
    private static final BigDecimal COMMISSION_RATE =
            new BigDecimal("0.10");

    private final SellerApplicationRepository sellerApplicationRepository;
    private final OrderDetailRepository orderDetailRepository;

    /**
     * 판매자 정산 내역 요약 조회
     *
     * 처리 순서
     * 1. 승인된 판매자 확인
     * 2. 조회 기간 검증 및 기본값 설정
     * 3. 구매 확정 주문의 총매출 조회
     * 4. 플랫폼 수수료 계산
     * 5. 최종 정산 예정 금액 계산
     * 6. 응답 DTO 반환
     */
    public SellerSettlementResponse getSettlements(
            Long userId,
            SellerSettlementSearchRequest request
    ) {
        // 1. 승인된 판매자 정보를 조회한다.
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        // 2. 조회 시작일과 종료일의 기본값을 설정한다.
        LocalDate startDate =
                request.startDate() == null
                        ? LocalDate.now().withDayOfMonth(1)
                        : request.startDate();

        LocalDate endDate =
                request.endDate() == null
                        ? LocalDate.now()
                        : request.endDate();

        // 시작일이 종료일보다 늦으면 잘못된 조회 조건이다.
        if (startDate.isAfter(endDate)) {
            throw new CustomException(
                    ErrorCode.INVALID_SEARCH_PERIOD
            );
        }

        /*
         * 시작일 00:00부터 종료일 다음 날 00:00 직전까지 조회한다.
         *
         * 예:
         * startDate = 2026-07-01
         * endDate   = 2026-07-14
         *
         * 조회 범위:
         * 2026-07-01 00:00 이상
         * 2026-07-15 00:00 미만
         */
        LocalDateTime startDateTime =
                startDate.atStartOfDay();

        LocalDateTime endDateTime =
                endDate.plusDays(1).atStartOfDay();

        // 3. 구매 확정된 주문 상품의 총매출을 조회한다.
        Long confirmedSalesValue =
                orderDetailRepository
                        .sumTotalPriceBySellerAndDeliveryStatusAndPeriod(
                                seller.getId(),
                                DeliveryStatus.CONFIRMED,
                                startDateTime,
                                endDateTime
                        );

        BigDecimal totalSalesAmount =
                BigDecimal.valueOf(
                        confirmedSalesValue == null
                                ? 0L
                                : confirmedSalesValue
                );

        // 4. 플랫폼 수수료를 계산한다.
        BigDecimal commissionAmount =
                totalSalesAmount
                        .multiply(COMMISSION_RATE)
                        .setScale(0, RoundingMode.DOWN);

        // 5. 수수료를 제외한 판매자 정산 예정 금액을 계산한다.
        BigDecimal expectedSettlementAmount =
                totalSalesAmount.subtract(commissionAmount);

        /*
         * 아직 별도의 Settlement 테이블이 없으므로
         * 완료 정산 금액은 임시로 0원 처리한다.
         *
         * 정산 테이블 병합 후에는 COMPLETED 상태의 합계를 조회한다.
         */
        BigDecimal completedSettlementAmount =
                BigDecimal.ZERO;

        // 6. 정산 요약 결과를 반환한다.
        return new SellerSettlementResponse(
                startDate,
                endDate,
                totalSalesAmount,
                commissionAmount,
                expectedSettlementAmount,
                completedSettlementAmount
        );
    }

    /**
     * 사용자의 가장 최근 입점 신청이 승인 상태인지 확인한다.
     */
    private SellerApplication getApprovedSellerApplication(
            Long userId
    ) {
        SellerApplication application =
                sellerApplicationRepository
                        .findFirstByUser_IdOrderByCreatedAtDesc(userId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SELLER_NOT_APPROVED
                                )
                        );

        if (application.getStatus()
                != SellerApplicationStatus.APPROVED) {
            throw new CustomException(
                    ErrorCode.SELLER_NOT_APPROVED
            );
        }

        return application;
    }
}