package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.seller.entity.SellerApplication;

/** GET /api/v1/seller/me - 판매자 마이페이지 회원정보 (프론트 seller-mypage-edit.js 대응) */
public record SellerProfileResponse(
        String username,
        String email,
        String businessName,
        String businessNumber,
        String representativeName,
        String managerPhone,
        String businessAddress,
        String status
) {
    public static SellerProfileResponse from(SellerApplication application) {
        return new SellerProfileResponse(
                application.getUser().getUsername(),
                application.getUser().getEmail(),
                application.getBusinessName(),
                application.getBusinessRegistrationNumber(),
                application.getRepresentativeName(),
                application.getContactNumber(),
                application.getBusinessAddress(),
                application.getStatus().name()
        );
    }
}
