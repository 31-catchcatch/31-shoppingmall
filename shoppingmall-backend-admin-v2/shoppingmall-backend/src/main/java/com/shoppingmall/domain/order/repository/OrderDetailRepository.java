package com.shoppingmall.domain.order.repository;

import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.entity.OrderDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderDetailRepository
        extends JpaRepository<OrderDetail, Long> {

    /**
     * 판매자에게 들어온 주문 상품 목록 조회
     *
     * Product.seller가 SellerApplication 타입이므로
     * SellerApplication ID를 기준으로 조회한다.
     */
    Page<OrderDetail>
    findAllByProduct_Seller_IdOrderByCreatedAtDesc(
            Long sellerApplicationId,
            Pageable pageable
    );

    /**
     * 배송 상태별 판매자 주문 목록 조회
     */
    Page<OrderDetail>
    findAllByProduct_Seller_IdAndDeliveryStatusOrderByCreatedAtDesc(
            Long sellerApplicationId,
            DeliveryStatus deliveryStatus,
            Pageable pageable
    );

    /**
     * 판매자 소유권까지 포함한 주문 상세 조회
     */
    Optional<OrderDetail> findByIdAndProduct_Seller_Id(
            Long orderDetailId,
            Long sellerApplicationId
    );

    /**
     * 사용자의 주문에 포함된 주문 상세 조회
     */
    Optional<OrderDetail> findByIdAndOrder_User_Id(
            Long orderDetailId,
            Long userId
    );

    /**
     * 특정 기간 동안 판매자에게 접수된 신규 주문 상품 수 조회
     */
    long countByProduct_Seller_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long sellerApplicationId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    /**
     * 특정 기간 동안 판매자의 유효 주문 상품 매출 합계 조회
     *
     * 취소·환불 건을 제외하기 위해
     * 매출로 인정할 배송 상태 목록을 전달받는다.
     */
    @Query("""
            select coalesce(sum(od.totalPrice), 0)
            from OrderDetail od
            where od.product.seller.id = :sellerId
              and od.deliveryStatus in :statuses
              and od.createdAt >= :startDateTime
              and od.createdAt < :endDateTime
            """)
    Long sumTotalPriceBySellerAndPeriod(
            @Param("sellerId")
            Long sellerApplicationId,

            @Param("statuses")
            Collection<DeliveryStatus> statuses,

            @Param("startDateTime")
            LocalDateTime startDateTime,

            @Param("endDateTime")
            LocalDateTime endDateTime
    );

    /**
     * 특정 기간 동안 특정 배송 상태인 주문 상품의 매출 합계 조회
     *
     * SellerSettlementService에서 구매 확정(CONFIRMED)된
     * 주문 상품의 정산 대상 매출을 계산할 때 사용한다.
     */
    @Query("""
            select coalesce(sum(od.totalPrice), 0)
            from OrderDetail od
            where od.product.seller.id = :sellerId
              and od.deliveryStatus = :deliveryStatus
              and od.confirmedAt >= :startDateTime
              and od.confirmedAt < :endDateTime
            """)
    Long sumTotalPriceBySellerAndDeliveryStatusAndPeriod(
            @Param("sellerId")
            Long sellerApplicationId,

            @Param("deliveryStatus")
            DeliveryStatus deliveryStatus,

            @Param("startDateTime")
            LocalDateTime startDateTime,

            @Param("endDateTime")
            LocalDateTime endDateTime
    );

    /**
     * 플랫폼 전체(모든 판매자) 기간 매출 합계 조회 - 관리자 정산 대시보드용.
     */
    @Query("""
            select coalesce(sum(od.totalPrice), 0)
            from OrderDetail od
            where od.deliveryStatus = :deliveryStatus
              and od.confirmedAt >= :startDateTime
              and od.confirmedAt < :endDateTime
            """)
    Long sumTotalPriceByDeliveryStatusAndPeriod(
            @Param("deliveryStatus")
            DeliveryStatus deliveryStatus,

            @Param("startDateTime")
            LocalDateTime startDateTime,

            @Param("endDateTime")
            LocalDateTime endDateTime
    );

    /**
     * 특정 기간 동안 판매자의 유효 주문 상품 총 판매 수량 조회
     */
    @Query("""
            select coalesce(sum(od.quantity), 0)
            from OrderDetail od
            where od.product.seller.id = :sellerId
              and od.deliveryStatus in :statuses
              and od.createdAt >= :startDateTime
              and od.createdAt < :endDateTime
            """)
    Long sumQuantityBySellerAndPeriod(
            @Param("sellerId")
            Long sellerApplicationId,

            @Param("statuses")
            Collection<DeliveryStatus> statuses,

            @Param("startDateTime")
            LocalDateTime startDateTime,

            @Param("endDateTime")
            LocalDateTime endDateTime
    );

    /**
     * 특정 기간 동안 판매자의 유효 주문 상품 건수 조회
     */
    @Query("""
            select count(od)
            from OrderDetail od
            where od.product.seller.id = :sellerId
              and od.deliveryStatus in :statuses
              and od.createdAt >= :startDateTime
              and od.createdAt < :endDateTime
            """)
    Long countSalesBySellerAndPeriod(
            @Param("sellerId")
            Long sellerApplicationId,

            @Param("statuses")
            Collection<DeliveryStatus> statuses,

            @Param("startDateTime")
            LocalDateTime startDateTime,

            @Param("endDateTime")
            LocalDateTime endDateTime
    );

    /**
     * 특정 기간 동안 판매자의 상품별 판매 통계 조회
     *
     * 반환 배열 구조
     * row[0] 상품 ID
     * row[1] 상품명
     * row[2] 주문 상세 건수
     * row[3] 총 판매 수량
     * row[4] 총매출
     */
    @Query("""
            select
                od.product.id,
                od.productName,
                count(od),
                coalesce(sum(od.quantity), 0),
                coalesce(sum(od.totalPrice), 0)
            from OrderDetail od
            where od.product.seller.id = :sellerId
              and od.deliveryStatus in :statuses
              and od.createdAt >= :startDateTime
              and od.createdAt < :endDateTime
            group by od.product.id, od.productName
            order by sum(od.totalPrice) desc
            """)
    List<Object[]> findProductSalesBySellerAndPeriod(
            @Param("sellerId")
            Long sellerApplicationId,

            @Param("statuses")
            Collection<DeliveryStatus> statuses,

            @Param("startDateTime")
            LocalDateTime startDateTime,

            @Param("endDateTime")
            LocalDateTime endDateTime
    );
}