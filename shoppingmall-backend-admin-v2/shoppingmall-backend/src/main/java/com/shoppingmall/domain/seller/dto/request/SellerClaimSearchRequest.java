package com.shoppingmall.domain.seller.dto.request;

public record SellerClaimSearchRequest(

        // RETURN, EXCHANGE 등
        String claimType,

        // REQUESTED, ACCEPTED, REJECTED 등
        String status,

        Integer page,
        Integer size

) {
}