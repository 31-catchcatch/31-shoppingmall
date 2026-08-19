package com.shoppingmall.domain.order.dto.response;

import com.shoppingmall.domain.order.entity.OrderDetail;

public record OrderDetailResponse(

        Long orderDetailId,
        Long productId,
        String productName,
        String thumbnailUrl,   // 상품의 현재 대표 썸네일 (없으면 null). 주문 당시 이미지 스냅샷이 아니다.
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
                detail.getProduct().getThumbnailUrl(),
                detail.getUnitPrice(),
                detail.getQuantity(),
                detail.getTotalPrice(),
                detail.getDeliveryStatus().name(),
                detail.getCourierCompany(),
                detail.getTrackingNumber()
        );
    }
}