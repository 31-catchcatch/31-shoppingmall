package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.claim.entity.ClaimStatus;
import com.shoppingmall.domain.claim.repository.ClaimRepository;
import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.repository.OrderDetailRepository;
import com.shoppingmall.domain.qna.repository.QnaRepository;
import com.shoppingmall.domain.seller.dto.response.SellerDashboardResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
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
 * 판매자 대시보드 요약 정보를 조회하는 서비스
 *
 * 담당 API
 * GET /api/v1/seller/dashboard
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDashboardService {

    private final SellerApplicationRepository sellerApplicationRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final QnaRepository qnaRepository;
    private final ClaimRepository claimRepository;

    /**
     * 판매자 대시보드 요약 조회
     *
     * 처리 순서
     * 1. 승인된 판매자 확인
     * 2. 오늘의 시작·종료 시각 계산
     * 3. 오늘 매출 합계 조회
     * 4. 오늘 신규 주문 수 조회
     * 5. 미답변 Q&A 수 조회
     * 6. 처리 대기 클레임 수 조회
     * 7. 응답 DTO 반환
     *
     * @param userId 로그인한 사용자 ID
     * @return 판매자 대시보드 요약 정보
     */
    public SellerDashboardResponse getDashboard(Long userId) {

        // 1. 승인된 판매자 신청 정보를 조회한다.
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        // 2. 오늘 00:00:00부터 내일 00:00:00 직전까지 조회한다.
        LocalDateTime startOfToday =
                LocalDate.now().atStartOfDay();

        LocalDateTime startOfTomorrow =
                startOfToday.plusDays(1);

        /*
         * 취소·환불된 주문 상품은 매출에서 제외한다.
         */
        List<DeliveryStatus> salesStatuses = List.of(
                DeliveryStatus.PAYMENT_COMPLETED,
                DeliveryStatus.PREPARING,
                DeliveryStatus.SHIPPING,
                DeliveryStatus.DELIVERED,
                DeliveryStatus.CONFIRMED
        );

        // 3. 오늘 생성된 주문 상품의 매출 합계를 조회한다.
        Long todaySalesValue =
                orderDetailRepository.sumTotalPriceBySellerAndPeriod(
                        seller.getId(),
                        salesStatuses,
                        startOfToday,
                        startOfTomorrow
                );

        /*
         * SUM 결과는 데이터가 없으면 null이 될 수 있으므로
         * null일 경우 0원으로 처리한다.
         */
        BigDecimal todaySales = BigDecimal.valueOf(
                todaySalesValue == null
                        ? 0L
                        : todaySalesValue
        );

        // 4. 오늘 새로 접수된 주문 상품 수를 조회한다.
        long newOrderCount =
                orderDetailRepository
                        .countByProduct_Seller_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                seller.getId(),
                                startOfToday,
                                startOfTomorrow
                        );

        // 5. 판매자 상품에 등록된 미답변 문의 수를 조회한다.
        long unansweredQnaCount =
                qnaRepository
                        .countByProduct_Seller_IdAndAnsweredFalseAndDeletedFalse(
                                seller.getId()
                        );

        // 6. 아직 판매자가 확인하지 않은 클레임 수를 조회한다.
        long pendingClaimCount =
                claimRepository
                        .countByOrderDetail_Product_Seller_IdAndStatus(
                                seller.getId(),
                                ClaimStatus.REQUESTED
                        );

        // 7. 조회 결과를 응답 DTO로 반환한다.
        return new SellerDashboardResponse(
                todaySales,
                newOrderCount,
                unansweredQnaCount,
                pendingClaimCount
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