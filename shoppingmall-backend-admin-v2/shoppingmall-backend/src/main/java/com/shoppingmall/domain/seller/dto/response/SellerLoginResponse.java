package com.shoppingmall.domain.seller.dto.response;

public record SellerLoginResponse(

        String accessToken,
        String refreshToken,
        String tokenType,
        Long sellerId,
        String businessName

) {
}