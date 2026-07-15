package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.repository.OrderDetailRepository;
import com.shoppingmall.domain.seller.dto.request.SellerSalesSearchRequest;
import com.shoppingmall.domain.seller.dto.response.SellerProductSalesResponse;
import com.shoppingmall.domain.seller.dto.response.SellerSalesResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 판매자 매출 통계 조회 서비스
 *
 * 담당 API
 * GET /api/v1/seller/sales
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerSalesService {

    private final OrderDetailRepository orderDetailRepository;
    private final SellerApplicationRepository sellerApplicationRepository;

    /**
     * 기간별 판매자 매출 통계 조회
     *
     * 처리 순서
     * 1. 승인된 판매자 확인
     * 2. 조회 기간 기본값 설정 및 검증
     * 3. 매출로 인정할 주문 상태 설정
     * 4. 총매출 조회
     * 5. 판매 건수 및 수량 조회
     * 6. 상품별 판매 통계 조회
     * 7. 응답 DTO 반환
     */
    public SellerSalesResponse getSales(
            Long userId,
            SellerSalesSearchRequest request
    ) {
        // 1. 승인된 판매자인지 확인한다.
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        /*
         * 2. 날짜를 입력하지 않은 경우
         * 이번 달 1일부터 오늘까지를 기본 조회 기간으로 사용한다.
         */
        LocalDate startDate =
                request.startDate() == null
                        ? LocalDate.now().withDayOfMonth(1)
                        : request.startDate();

        LocalDate endDate =
                request.endDate() == null
                        ? LocalDate.now()
                        : request.endDate();

        if (startDate.isAfter(endDate)) {
            throw new CustomException(
                    ErrorCode.INVALID_SEARCH_PERIOD
            );
        }

        LocalDateTime startDateTime =
                startDate.atStartOfDay();

        /*
         * 종료일도 포함하기 위해
         * 종료일 다음 날 00시 미만으로 조회한다.
         */
        LocalDateTime endDateTime =
                endDate.plusDays(1).atStartOfDay();

        /*
         * 3. 취소 및 환불 상태는 매출에서 제외한다.
         *
         * 현재 구현에서는 결제 완료 이후의 정상 주문을
         * 매출 통계에 포함한다.
         */
        List<DeliveryStatus> salesStatuses = List.of(
                DeliveryStatus.PAYMENT_COMPLETED,
                DeliveryStatus.PREPARING,
                DeliveryStatus.SHIPPING,
                DeliveryStatus.DELIVERED,
                DeliveryStatus.CONFIRMED
        );

        // 4. 기간 내 총매출을 조회한다.
        Long totalSalesAmount =
                orderDetailRepository
                        .sumTotalPriceBySellerAndPeriod(
                                seller.getId(),
                                salesStatuses,
                                startDateTime,
                                endDateTime
                        );

        // 5. 주문 상품 건수와 총 판매 수량을 조회한다.
        Long orderCount =
                orderDetailRepository
                        .countSalesBySellerAndPeriod(
                                seller.getId(),
                                salesStatuses,
                                startDateTime,
                                endDateTime
                        );

        Long totalSoldQuantity =
                orderDetailRepository
                        .sumQuantityBySellerAndPeriod(
                                seller.getId(),
                                salesStatuses,
                                startDateTime,
                                endDateTime
                        );

        // 6. 상품별 집계 결과를 조회한다.
        List<Object[]> productSalesData =
                orderDetailRepository
                        .findProductSalesBySellerAndPeriod(
                                seller.getId(),
                                salesStatuses,
                                startDateTime,
                                endDateTime
                        );

        List<SellerProductSalesResponse> productSales =
                productSalesData.stream()
                        .map(this::toProductSalesResponse)
                        .toList();

        // 7. 전체 통계 결과를 반환한다.
        return new SellerSalesResponse(
                startDate,
                endDate,
                totalSalesAmount == null ? 0L : totalSalesAmount,
                orderCount == null ? 0L : orderCount,
                totalSoldQuantity == null ? 0L : totalSoldQuantity,
                productSales
        );
    }

    /**
     * Repository의 상품별 Object 배열 결과를
     * 응답 DTO로 변환한다.
     */
    private SellerProductSalesResponse toProductSalesResponse(
            Object[] row
    ) {
        return new SellerProductSalesResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).longValue(),
                ((Number) row[3]).longValue(),
                ((Number) row[4]).longValue()
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