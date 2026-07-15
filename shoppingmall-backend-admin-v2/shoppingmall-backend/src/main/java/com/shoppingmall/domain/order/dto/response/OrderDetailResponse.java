package com.shoppingmall.domain.order.dto.response;

import com.shoppingmall.domain.order.entity.OrderDetail;

public record OrderDetailResponse(

        Long orderDetailId,
        Long productId,
        String productName,
        Integer unitPrice,
        Integer quantity,
        Integer totalPrice,
        String deliveryStatus,
        String courierCompany,
        String trackingNumber

) {

    public static OrderDetailResponse from(OrderDetail detail) {
        return new OrderDetailResponse(
                detail.getId(),
                detail.getProduct().getId(),
                detail.getProductName(),
                detail.getUnitPrice(),
                detail.getQuantity(),
                detail.getTotalPrice(),
                detail.getDeliveryStatus().name(),
                detail.getCourierCompany(),
                detail.getTrackingNumber()
        );
    }
}