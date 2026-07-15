package com.shoppingmall.domain.order.dto.response;

import com.shoppingmall.domain.order.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(

        Long orderId,
        String orderNumber,
        String orderStatus,
        Integer totalProductAmount,
        Integer couponDiscountAmount,
        Integer usedPointAmount,
        Integer finalPaymentAmount,
        String receiverName,
        String receiverPhone,
        String zipCode,
        String address,
        String addressDetail,
        String deliveryRequest,
        LocalDateTime createdAt,
        List<OrderDetailResponse> orderDetails

) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getTotalProductAmount(),
                order.getCouponDiscountAmount(),
                order.getUsedPointAmount(),
                order.getFinalPaymentAmount(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getZipCode(),
                order.getAddress(),
                order.getAddressDetail(),
                order.getDeliveryRequest(),
                order.getCreatedAt(),
                order.getOrderDetails()
                        .stream()
                        .map(OrderDetailResponse::from)
                        .toList()
        );
    }
}