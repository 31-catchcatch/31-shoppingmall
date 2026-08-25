package com.shoppingmall.domain.order.service;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderDetail;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.payment.entity.Payment;
import com.shoppingmall.domain.payment.repository.PaymentRepository;
import com.shoppingmall.domain.point.service.PointService;
import com.shoppingmall.domain.product.repository.ProductOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제까지 이어지지 않은 주문을 만료시킨다.
 *
 * <p>주문 생성 시점에 재고를 차감(OrderService:141)하므로, 결제창을 닫거나 승인이 거절되어
 * PENDING 으로 남은 주문은 재고를 계속 점유한다. 사용자가 명시적으로 취소해야만 복원되는데
 * 브라우저를 그냥 닫으면 그 호출도 오지 않는다. 반복하면 결제 없이 재고를 소진시킬 수 있다.
 *
 * <p>OrderService.cancelOrder 와 동일한 복원 순서를 따른다. (재고 → 쿠폰 → 포인트 → 결제원장)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireScheduler {

    /** 결제 대기 허용 시간. 토스 결제창 유효시간보다 넉넉하게 잡는다. */
    private static final int EXPIRE_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final ProductOptionRepository productOptionRepository;
    private final PaymentRepository paymentRepository;
    private final PointService pointService;

    @Scheduled(fixedDelay = 300_000)   // 5분마다
    @Transactional
    public void expirePendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRE_MINUTES);

        List<Order> targets =
                orderRepository.findAllByStatusAndCreatedAtBefore(OrderStatus.PENDING, cutoff);
        if (targets.isEmpty()) {
            return;
        }

        for (Order order : targets) {
            try {
                expire(order);
            } catch (Exception e) {
                // 한 건이 실패해도 나머지는 계속 처리한다.
                log.error("[EXPIRE] 미결제 주문 만료 실패. orderNumber={}", order.getOrderNumber(), e);
            }
        }
        log.info("[EXPIRE] 미결제 주문 {}건 만료 처리 (기준: {}분 경과)", targets.size(), EXPIRE_MINUTES);
    }

    private void expire(Order order) {
        // 1) 재고 복구
        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.getProductOption() != null) {
                productOptionRepository.restoreStock(
                        detail.getProductOption().getId(), detail.getQuantity());
            }
            detail.cancel();
        }

        // 2) 쿠폰 복원
        if (order.getUsedCoupon() != null) {
            order.getUsedCoupon().restore();
        }

        // 3) 포인트 복원
        if (order.getUsedPointAmount() > 0) {
            pointService.adjustPoint(
                    order.getUser().getId(),
                    order.getUsedPointAmount(),
                    "미결제 주문 만료로 인한 포인트 복원 (주문번호: " + order.getOrderNumber() + ")");
        }

        // 4) READY 로 남은 결제 원장 정리
        paymentRepository.findByOrder_Id(order.getId()).ifPresent(Payment::cancelPayment);

        order.cancel();
        log.info("[EXPIRE] 미결제 주문 만료. orderNumber={}, createdAt={}",
                order.getOrderNumber(), order.getCreatedAt());
    }
}
