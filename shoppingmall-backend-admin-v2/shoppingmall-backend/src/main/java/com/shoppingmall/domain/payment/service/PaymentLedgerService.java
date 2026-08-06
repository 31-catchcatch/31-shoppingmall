package com.shoppingmall.domain.payment.service;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.payment.dto.request.PaymentConfirmRequest;
import com.shoppingmall.domain.payment.dto.response.PaymentResponse;
import com.shoppingmall.domain.payment.entity.Payment;
import com.shoppingmall.domain.payment.entity.PaymentStatus;
import com.shoppingmall.domain.payment.repository.PaymentRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토스 결제 승인 과정에서 DB 저장(payments/orders)을 다루는 서비스.
 *
 * <p><b>왜 PaymentService 에서 분리하는가</b><br>
 * 토스 승인은 외부 HTTP 호출이라 수 초가 걸릴 수 있다. 이걸 트랜잭션 안에서 부르면
 * (1) 그동안 DB 커넥션을 붙잡고 있게 되고,
 * (2) 승인 후 예외가 나면 롤백되면서 "토스는 승인됐는데 우리 DB엔 흔적이 없는" 상태가 된다.
 * 그래서 DB 구간(READY 저장 / PAID / FAILED)을 여기에 몰아넣고 각각 독립 트랜잭션으로 커밋하며,
 * 외부 호출은 트랜잭션 밖(PaymentService.confirmPayment)에서 한다.
 *
 * <p>REQUIRES_NEW 를 쓰되 반드시 다른 빈에서 호출해야 한다. 같은 클래스 안에서 호출하면
 * Spring 프록시를 거치지 않아(self-invocation) 전파 속성이 통째로 무시된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentLedgerService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /** 승인 요청 직전 상태. 토스에 보낼 금액과 저장할 결제 금액을 담는다. */
    public record Prepared(Long paymentId, int amount) {
    }

    /**
     * 주문을 검증하고 결제 원장을 READY 로 커밋한다. (토스 호출 전)
     *
     * @return 승인에 사용할 paymentId 와 결제 금액
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Prepared prepare(Long userId, PaymentConfirmRequest request) {
        // 1. 주문 조회 - PK 가 아니라 주문번호로 찾는다 (토스 orderId 는 6~64자 제약이라 PK 로 못 쓴다)
        Order order = orderRepository.findByOrderNumber(request.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 소유자 검증 - 남의 주문번호를 알아내도 승인시킬 수 있어선 안 된다
        if (!order.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 3. 중복 승인 가드 - 결제 대기 상태의 주문만 승인할 수 있다
        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("[TOSS] 결제 대기 상태가 아닌 주문에 승인 시도. orderNumber={}, status={}",
                    order.getOrderNumber(), order.getStatus());
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_CONFIRMED);
        }

        // 4. [취약: 실습용 - 파라미터(금액) 변조]
        //    원래는 order.getFinalPaymentAmount() 와 대조해 불일치 시 토스 호출 전에 차단했다.
        //    지금은 그 대조를 제거하고 클라이언트가 보낸 amount 를 그대로 신뢰한다.
        int expectedAmount = request.getAmount();

        // 5. 이 paymentKey 를 다른 주문이 이미 선점하는지 확인 (pg_transaction_id 는 UNIQUE)
        paymentRepository.findByPgTransactionId(request.getPaymentKey())
                .filter(p -> !p.getOrder().getId().equals(order.getId()))
                .ifPresent(p -> {
                    log.warn("[TOSS] 다른 주문이 사용 중인 paymentKey. orderNumber={}", order.getOrderNumber());
                    throw new CustomException(ErrorCode.PAYMENT_ALREADY_CONFIRMED);
                });

        // 6. 결제 원장 READY 저장
        //    payments.order_id 는 UNIQUE 인덱스가 있어 주문당 한 행뿐이므로,
        //    승인 실패 후 재시도할 때는 새 행을 만들지 말고 기존 행을 재사용해야 한다.
        Payment payment = paymentRepository.findByOrder_Id(order.getId())
                .map(existing -> {
                    if (existing.getStatus() == PaymentStatus.PAID) {
                        throw new CustomException(ErrorCode.PAYMENT_ALREADY_CONFIRMED);
                    }
                    existing.prepareRetry(request.getPaymentKey(), expectedAmount);
                    return existing;
                })
                .orElseGet(() -> paymentRepository.save(
                        Payment.ready(order, request.getPaymentKey(), expectedAmount)));

        // flush 로 강제해 UNIQUE 위반 같은 제약 오류가 토스 호출 전에 드러나게 한다
        paymentRepository.flush();

        return new Prepared(payment.getId(), expectedAmount);
    }

    /** 토스 승인 성공 -> 결제 PAID, 주문 PAID. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentResponse markPaid(Long paymentId, String payMethod) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        payment.markPaid(payMethod);
        payment.getOrder().completePayment();

        return PaymentResponse.from(payment);
    }

    /**
     * 토스가 거절했거나 통신에 실패 -> 결제 FAILED.
     *
     * 주문은 PENDING 그대로 두어 사용자가 다시 결제할 수 있게 한다.
     * (재시도 시 같은 행을 prepareRetry 로 재사용한다)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long paymentId) {
        paymentRepository.findById(paymentId).ifPresent(Payment::markFailed);
    }
}
