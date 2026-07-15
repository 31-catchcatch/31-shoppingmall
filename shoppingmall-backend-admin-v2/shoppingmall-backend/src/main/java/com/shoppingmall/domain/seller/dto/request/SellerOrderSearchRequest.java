package com.shoppingmall.domain.seller.dto.request;

public record SellerOrderSearchRequest(

        String deliveryStatus,
        Integer page,
        Integer size

) {
}