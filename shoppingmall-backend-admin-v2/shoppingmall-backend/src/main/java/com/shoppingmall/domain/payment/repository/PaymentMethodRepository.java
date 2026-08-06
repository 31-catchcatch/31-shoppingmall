package com.shoppingmall.domain.payment.repository;

import com.shoppingmall.domain.payment.entity.PaymentMethod;
import com.shoppingmall.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findAllByUserOrderByCreatedAtDesc(User user);

    Optional<PaymentMethod> findByIdAndUser_Id(Long id, Long userId);

    Optional<PaymentMethod> findByUserAndDefaultMethodTrue(User user);
}
