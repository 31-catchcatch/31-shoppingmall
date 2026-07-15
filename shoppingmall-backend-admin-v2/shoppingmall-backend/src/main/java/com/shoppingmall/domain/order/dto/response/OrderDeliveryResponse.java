package com.shoppingmall.domain.order.dto.response;

import com.shoppingmall.domain.order.entity.OrderDetail;

import java.time.LocalDateTime;

public record OrderDeliveryResponse(

        Long orderDetailId,
        String courierCompany,
        String trackingNumber,
        String deliveryStatus,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt

) {

    public static OrderDeliveryResponse from(OrderDetail detail) {
        return new OrderDeliveryResponse(
                detail.getId(),
                detail.getCourierCompany(),
                detail.getTrackingNumber(),
                detail.getDeliveryStatus().name(),
                detail.getShippedAt(),
                detail.getDeliveredAt()
        );
    }
}