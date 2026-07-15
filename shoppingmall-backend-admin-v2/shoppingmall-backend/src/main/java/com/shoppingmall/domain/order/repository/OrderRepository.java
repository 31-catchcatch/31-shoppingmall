package com.shoppingmall.domain.order.repository;

import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 일반 사용자의 주문 내역 조회
     */
    Page<Order> findAllByUser_IdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    /**
     * 본인의 주문 단건 조회
     */
    Optional<Order> findByIdAndUser_Id(
            Long orderId,
            Long userId
    );

    /**
     * 주문번호 조회
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findAllByUser_IdAndStatusOrderByCreatedAtDesc(
            Long userId,
            OrderStatus status,
            Pageable pageable
    );
}