package com.shoppingmall.domain.payment.repository;

import com.shoppingmall.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // PG사 거래 고유 ID로 중복 결제 시도 사전 방지 및 조회
    Optional<Payment> findByPgTransactionId(String pgTransactionId);
}