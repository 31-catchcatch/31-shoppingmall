package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.order.entity.OrderDetail;
import org.springframework.data.domain.Page;

import java.util.List;

public record SellerOrderListResponse(

        List<SellerOrderResponse> orders,
        int page,
        int size,
        long totalElements,
        int totalPages

) {

    public static SellerOrderListResponse from(
            Page<OrderDetail> orderPage
    ) {
        return new SellerOrderListResponse(
                orderPage.getContent()
                        .stream()
                        .map(SellerOrderResponse::from)
                        .toList(),
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }
}