package com.shoppingmall.domain.settlement.repository;

import com.shoppingmall.domain.settlement.entity.Settlement;
import com.shoppingmall.domain.settlement.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 정산 리포지토리.
 *
 * 기간 필터는 정산 생성 시각(created_at) 기준이며, 상한(end)은 서비스에서
 * endDate.plusDays(1).atStartOfDay() 로 넘어오므로 미포함(<)으로 비교한다.
 * 모든 합계는 coalesce(..., 0) 으로 감싸 결과가 null 이 되지 않도록 한다.
 */
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /** 동일 주문상세로 정산이 이미 생성되었는지(중복 정산 방지) */
    boolean existsByOrderDetail_Id(Long orderDetailId);

    // ===================== 판매자별 집계 =====================

    @Query("""
            select coalesce(sum(s.saleAmount), 0)
            from Settlement s
            where s.seller.id = :sellerId
              and s.createdAt >= :start and s.createdAt < :end
            """)
    Long sumSaleAmountBySeller(@Param("sellerId") Long sellerId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(sum(s.feeAmount), 0)
            from Settlement s
            where s.seller.id = :sellerId
              and s.createdAt >= :start and s.createdAt < :end
            """)
    Long sumFeeAmountBySeller(@Param("sellerId") Long sellerId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(sum(s.settlementAmount), 0)
            from Settlement s
            where s.seller.id = :sellerId
              and s.status = :status
              and s.createdAt >= :start and s.createdAt < :end
            """)
    Long sumSettlementAmountBySellerAndStatus(@Param("sellerId") Long sellerId,
                                              @Param("status") SettlementStatus status,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    // ===================== 플랫폼 전체 집계 =====================

    @Query("""
            select coalesce(sum(s.saleAmount), 0)
            from Settlement s
            where s.createdAt >= :start and s.createdAt < :end
            """)
    Long sumSaleAmountAll(@Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(sum(s.feeAmount), 0)
            from Settlement s
            where s.createdAt >= :start and s.createdAt < :end
            """)
    Long sumFeeAmountAll(@Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    @Query("""
            select coalesce(sum(s.settlementAmount), 0)
            from Settlement s
            where s.status = :status
              and s.createdAt >= :start and s.createdAt < :end
            """)
    Long sumSettlementAmountAllByStatus(@Param("status") SettlementStatus status,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);
}
