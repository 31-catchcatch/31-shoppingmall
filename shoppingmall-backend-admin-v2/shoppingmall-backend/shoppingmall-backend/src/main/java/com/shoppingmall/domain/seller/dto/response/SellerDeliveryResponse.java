package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.order.entity.OrderDetail;

import java.time.LocalDateTime;

/**
 * 판매자 배송 정보 변경 응답 DTO
 */
public record SellerDeliveryResponse(

        Long orderDetailId,
        String courierCompany,
        String trackingNumber,
        String deliveryStatus,
        LocalDateTime updatedAt

) {

    public static SellerDeliveryResponse from(
            OrderDetail orderDetail
    ) {
        return new SellerDeliveryResponse(
                orderDetail.getId(),
                orderDetail.getCourierCompany(),
                orderDetail.getTrackingNumber(),
                orderDetail.getDeliveryStatus().name(),
                orderDetail.getUpdatedAt()
        );
    }
}