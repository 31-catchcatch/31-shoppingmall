package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.order.entity.OrderDetail;

import java.time.LocalDateTime;

public record SellerOrderResponse(

        Long orderDetailId,
        Long orderId,
        Long productId,
        String productName,
        Integer unitPrice,
        Integer quantity,
        Integer totalPrice,
        String deliveryStatus,
        String courierCompany,
        String trackingNumber,
        String buyerName,
        String buyerUsername,
        LocalDateTime orderedAt

) {

    public static SellerOrderResponse from(
            OrderDetail orderDetail
    ) {
        return new SellerOrderResponse(
                orderDetail.getId(),
                orderDetail.getOrder().getId(),
                orderDetail.getProduct().getId(),
                orderDetail.getProductName(),
                orderDetail.getUnitPrice(),
                orderDetail.getQuantity(),
                orderDetail.getTotalPrice(),
                orderDetail.getDeliveryStatus().name(),
                orderDetail.getCourierCompany(),
                orderDetail.getTrackingNumber(),
                orderDetail.getOrder().getUser().getName(),
                orderDetail.getOrder().getUser().getUsername(),
                orderDetail.getCreatedAt()
        );
    }
}