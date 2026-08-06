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
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
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

    // ===== 토스 결제 승인(POST /payments/confirm) 전용 =====
    //
    // 위 @Builder 생성자는 기존 mock 경로(/payments/verify)가 쓰고 있어 손대지 않는다.
    // 토스 경로는 "승인 요청 전에 READY 로 먼저 저장"해야 하므로 아래 팩토리/전이 메서드를 쓴다.

    /**
     * 토스에 승인을 요청하기 직전의 결제 원장을 만든다.
     *
     * pgProvider/payMethod 는 NOT NULL 이라 비워둘 수 없어서 잠정값을 넣는다.
     * payMethod 는 승인 성공 시 토스 응답값으로 {@link #markPaid(String)} 에서 덮어쓴다.
     */
    public static Payment ready(Order order, String paymentKey, int amount) {
        Payment payment = new Payment();
        payment.order = order;
        payment.pgProvider = "TOSS";
        payment.payMethod = "CARD"; // Tier 1 은 카드만. 승인 응답이 오면 실제 값으로 교체된다
        payment.pgTransactionId = paymentKey;
        payment.amount = amount;
        payment.status = PaymentStatus.READY;
        return payment;
    }

    /**
     * 승인에 실패했던(또는 승인 요청 도중 이탈한) 결제를 같은 행에서 다시 시도한다.
     *
     * ⚠️ payments 테이블은 order_id 에 UNIQUE 인덱스가 있어(@OneToOne) 주문당 결제 행이 하나뿐이다.
     * 재시도할 때 새 행을 INSERT 하면 unique 위반으로 터지므로 반드시 기존 행을 재사용해야 한다.
     * 토스는 결제창을 다시 띄울 때마다 새 paymentKey 를 발급하므로 그 값으로 갱신한다.
     */
    public void prepareRetry(String paymentKey, int amount) {
        this.pgTransactionId = paymentKey;
        this.amount = amount;
        this.status = PaymentStatus.READY;
    }

    /** 토스 승인 성공. 결제수단은 클라이언트 신고값이 아니라 토스 응답값을 쓴다. */
    public void markPaid(String payMethod) {
        if (payMethod != null && !payMethod.isBlank()) {
            this.payMethod = payMethod;
        }
        this.status = PaymentStatus.PAID;
    }

    /** 토스가 거절했거나 통신에 실패한 경우. 주문은 PENDING 으로 남아 재시도할 수 있다. */
    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }
}