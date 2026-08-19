package com.shoppingmall.domain.order.dto.response;

import com.shoppingmall.domain.order.entity.Order;
import org.springframework.data.domain.Page;

import java.util.List;

public record OrderListResponse(

        List<OrderResponse> orders,
        int page,
        int size,
        long totalElements,
        int totalPages

) {

    public static OrderListResponse from(Page<Order> orderPage) {
        return new OrderListResponse(
                orderPage.getContent()
                        .stream()
                        .map(OrderResponse::from)
                        .toList(),
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }
}