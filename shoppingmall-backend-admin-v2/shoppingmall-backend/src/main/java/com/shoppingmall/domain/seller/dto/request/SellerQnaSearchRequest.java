package com.shoppingmall.domain.seller.dto.request;

public record SellerQnaSearchRequest(

        Long productId,
        Boolean answered,
        Integer page,
        Integer size

) {
}