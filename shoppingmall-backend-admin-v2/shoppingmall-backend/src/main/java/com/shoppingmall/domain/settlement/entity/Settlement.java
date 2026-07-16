package com.shoppingmall.domain.settlement.entity;

import com.shoppingmall.domain.order.entity.OrderDetail;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 정산 내역.
 *
 * 구매확정(OrderService.confirmPurchase) 시점에 주문상세(OrderDetail) 1건당 1개 생성된다.
 * 판매 금액에서 플랫폼 수수료를 차감한 실지급 금액을 미리 계산해 저장해 두고,
 * 판매자/관리자 정산 대시보드에서 기간별로 합계를 집계한다.
 *
 * 금액은 프로젝트 공통 규칙(Product.price, OrderDetail.totalPrice 등)에 맞춰 원 단위 Integer 로 저장한다.
 */
@Getter
@Entity
@Table(name = "settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseTimeEntity {

    /** 플랫폼 기본 수수료율 10% (판매자/관리자 정산 서비스와 동일 기준) */
    public static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.10");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 정산 대상 판매자(승인된 입점 신청서 기준). Product.seller 와 동일하게 SellerApplication 을 참조한다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerApplication seller;

    /** 정산의 원천이 되는 주문상세. 동일 주문상세로 중복 정산되지 않도록 unique. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_detail_id", nullable = false, unique = true)
    private OrderDetail orderDetail;

    /** 판매 금액(주문상세 총액) */
    @Column(name = "sale_amount", nullable = false)
    private Integer saleAmount;

    /** 적용된 수수료율 */
    @Column(name = "fee_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal feeRate;

    /** 수수료 금액 = saleAmount * feeRate (반올림) */
    @Column(name = "fee_amount", nullable = false)
    private Integer feeAmount;

    /** 실지급 정산 금액 = saleAmount - feeAmount */
    @Column(name = "settlement_amount", nullable = false)
    private Integer settlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    /** 관리자가 실제 지급 완료를 기록한 시각(미완료 시 null) */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder
    private Settlement(SellerApplication seller,
                       OrderDetail orderDetail,
                       Integer saleAmount,
                       BigDecimal feeRate) {
        this.seller = seller;
        this.orderDetail = orderDetail;
        this.saleAmount = saleAmount;
        this.feeRate = feeRate;
        this.feeAmount = BigDecimal.valueOf(saleAmount)
                .multiply(feeRate)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
        this.settlementAmount = saleAmount - this.feeAmount;
        this.status = SettlementStatus.PENDING;
    }

    /** PATCH /admin/settlements/{id}/complete - 지급 완료 처리(멱등: 이미 완료면 유지) */
    public void complete() {
        if (this.status == SettlementStatus.COMPLETED) {
            return;
        }
        this.status = SettlementStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
