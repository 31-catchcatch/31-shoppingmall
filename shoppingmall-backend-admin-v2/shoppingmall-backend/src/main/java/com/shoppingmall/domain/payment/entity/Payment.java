package com.shoppingmall.domain.payment.entity;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "pg_provider", nullable = false, length = 50)
    private String pgProvider; // 예: "TOSS", "KAKAO", "KCP"

    @Column(name = "pay_method", nullable = false, length = 50)
    private String payMethod; // 예: "CARD", "TRANS" (계좌이체)

    @Column(name = "pg_transaction_id", nullable = false, unique = true, length = 100)
    private String pgTransactionId; // PG사 혹은 포트원 결제 고유 거래 ID (imp_uid 등)

    @Column(nullable = false)
    private int amount; // 실제 PG 결제액 (포인트 공제 후 최종 청구된 신용카드 승인 가액)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Builder
    public Payment(Order order, String pgProvider, String payMethod, String pgTransactionId, int amount) {
        this.order = order;
        this.pgProvider = pgProvider;
        this.payMethod = payMethod;
        this.pgTransactionId = pgTransactionId;
        this.amount = amount;
        this.status = PaymentStatus.PAID; // 결제가 승인 완료되어 저장되므로 기본 PAID
    }

    // 환불 처리 시 상태 취소 전환 로직
    public void cancelPayment() {
        this.status = PaymentStatus.CANCELLED;
    }
}