package com.shoppingmall.domain.payment.service;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.payment.dto.request.PaymentVerifyRequest;
import com.shoppingmall.domain.payment.dto.response.PaymentResponse;
import com.shoppingmall.domain.payment.entity.Payment;
import com.shoppingmall.domain.payment.repository.PaymentRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResponse verifyAndSavePayment(Long userId, PaymentVerifyRequest request) {
        // 1. 위조 거래 검사 및 검증 대상 주문서 확보
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        // 보안 검증: 주문을 요청했던 원천 사용자와 결제를 승인하려는 사용자가 맞는지 파악
        if (!order.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 2. [가장 중요] 가액 대조 위변조 검사
        // Order 생성 시점에 쿠폰/포인트까지 반영해 계산해둔 최종 결제 금액과 PG 청구액이 일치하는지 검증
        int expectedAmount = order.getFinalPaymentAmount();
        if (expectedAmount != request.getAmount()) {
            throw new CustomException(ErrorCode.INVALID_INPUT); // "결제 위변조 검증 실패: 금액이 일치하지 않습니다."
        }

        // 3. 중복 승인 결제 시도 가드 처리
        paymentRepository.findByPgTransactionId(request.getPgTransactionId())
                .ifPresent(p -> {
                    throw new CustomException(ErrorCode.INVALID_INPUT); // "이미 결제 처리가 완료된 고유 거래 번호입니다."
                });

        // 4. 결제 성공 원장 테이블 빌드 및 영속 저장
        Payment payment = Payment.builder()
                .order(order)
                .pgTransactionId(request.getPgTransactionId())
                .pgProvider(request.getPgProvider() == null ? "UNKNOWN" : request.getPgProvider())
                .payMethod(request.getPayMethod())
                .amount(request.getAmount())
                .build();

        paymentRepository.save(payment);

        // 5. 결제 승인 완료 -> 주문 상태 전환 (PENDING -> PAID)
        order.completePayment();

        return PaymentResponse.from(payment);
    }
}