package com.shoppingmall.domain.payment.service;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.payment.client.TossPaymentClient;
import com.shoppingmall.domain.payment.client.TossPaymentException;
import com.shoppingmall.domain.payment.client.dto.TossConfirmResponse;
import com.shoppingmall.domain.payment.dto.request.PaymentConfirmRequest;
import com.shoppingmall.domain.payment.dto.request.PaymentVerifyRequest;
import com.shoppingmall.domain.payment.dto.response.PaymentConfigResponse;
import com.shoppingmall.domain.payment.dto.response.PaymentResponse;
import com.shoppingmall.domain.payment.entity.Payment;
import com.shoppingmall.domain.payment.repository.PaymentRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentLedgerService paymentLedgerService;
    private final TossPaymentClient tossPaymentClient;

    // 주입 방식은 이 프로젝트 관행을 따른다 (JwtTokenProvider, FileStorageService 와 동일한 @Value 필드 주입)
    @Value("${payment.toss.client-key:}")
    private String tossClientKey;

    @Value("${payment.toss.secret-key:}")
    private String tossSecretKey;

    @Value("${payment.toss.success-url:}")
    private String tossSuccessUrl;

    @Value("${payment.toss.fail-url:}")
    private String tossFailUrl;

    /**
     * 프론트가 결제창을 띄우는 데 필요한 환경값을 내려준다.
     * clientKey 는 공개값이므로 비로그인 상태에서도 조회 가능하다 (SecurityConfig permitAll).
     */
    public PaymentConfigResponse getPaymentConfig() {
        return PaymentConfigResponse.builder()
                .clientKey(tossClientKey)
                .successUrl(tossSuccessUrl)
                .failUrl(tossFailUrl)
                .build();
    }

    /**
     * 토스 결제 승인. 프론트가 결제창에서 받아온 paymentKey 를 서버가 토스에 직접 확인시킨다.
     *
     * <p>기존 {@link #verifyAndSavePayment}(mock)와 달리 클라이언트의 신고를 신뢰하지 않는다.
     * 결제 성사 여부·실제 승인 금액·결제수단을 모두 토스 응답에서 받아온다.
     *
     * <p><b>트랜잭션 주의</b>: NOT_SUPPORTED 로 클래스 레벨 @Transactional(readOnly=true) 를 무력화해
     * 외부 HTTP 호출이 트랜잭션 밖에서 일어나게 한다. DB 는 PaymentLedgerService 가
     * 독립 트랜잭션(REQUIRES_NEW)으로 나눠 커밋하므로, 토스가 승인한 뒤 무슨 일이 나도
     * READY 기록이 롤백으로 사라지지 않는다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        // 키가 없는 환경에서 조용히 실패하지 않도록 먼저 걸러낸다.
        // (팀원 환경에서 앱이 안 뜨는 걸 막으려고 설정 기본값을 빈 문자열로 뒀기 때문에 여기서 확인한다)
        if (tossSecretKey == null || tossSecretKey.isBlank()) {
            log.error("[TOSS] payment.toss.secret-key 가 설정되지 않아 결제 승인을 진행할 수 없습니다.");
            throw new CustomException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }

        // 1. 주문 검증 + 결제 원장 READY 커밋 (토스 호출 전에 흔적을 남긴다)
        PaymentLedgerService.Prepared prepared = paymentLedgerService.prepare(userId, request);

        // 2. 토스 승인 요청 - 금액은 클라이언트 값이 아니라 서버가 확정한 값을 보낸다
        TossConfirmResponse confirmed;
        try {
            confirmed = tossPaymentClient.confirm(
                    request.getPaymentKey(), request.getOrderId(), prepared.amount());

        } catch (TossPaymentException e) {
            paymentLedgerService.markFailed(prepared.paymentId());

            if (e.isNetworkError()) {
                // 배포 환경에서 WAS 의 외부 443 아웃바운드가 막히면 여기로 떨어진다.
                // 거절과 구분해서 봐야 원인을 빨리 찾을 수 있어 에러코드를 나눈다.
                log.error("[TOSS] 결제 승인 통신 실패. orderNumber={}", request.getOrderId(), e);
                throw new CustomException(ErrorCode.PAYMENT_GATEWAY_ERROR);
            }

            log.warn("[TOSS] 결제 승인 거절. orderNumber={}, code={}, message={}",
                    request.getOrderId(), e.getCode(), e.getMessage());
            throw new CustomException(ErrorCode.PAYMENT_CONFIRM_FAILED);
        }

        // 3. 실제 승인된 금액 대조. 여기서 어긋나면 토스에는 승인이 남아 있으므로 반드시 눈에 띄어야 한다.
        int approvedAmount = confirmed.getTotalAmount() == null ? -1 : confirmed.getTotalAmount();
        if (approvedAmount != prepared.amount()) {
            paymentLedgerService.markFailed(prepared.paymentId());
            log.error("[TOSS] 승인 금액 불일치 - 토스에는 승인이 남아 있어 수동 취소가 필요합니다. "
                            + "orderNumber={}, paymentKey={}, 서버={}, 승인={}",
                    request.getOrderId(), request.getPaymentKey(), prepared.amount(), approvedAmount);
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 4. 결제 완료 확정 (Payment=PAID, Order=PAID)
        PaymentResponse response = paymentLedgerService.markPaid(
                prepared.paymentId(), confirmed.getMethod());

        log.info("[TOSS] 결제 승인 완료. orderNumber={}, amount={}, method={}",
                request.getOrderId(), approvedAmount, confirmed.getMethod());

        return response;
    }

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