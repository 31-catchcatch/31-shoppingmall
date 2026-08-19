package com.shoppingmall.domain.admin.dto.response;

import com.shoppingmall.domain.seller.entity.Seller;
import com.shoppingmall.domain.seller.entity.SellerStatus;

import java.time.LocalDateTime;

/**
 * GET /admin/sellers 응답.
 *
 * 프론트(admin-sellers.js)의 mapRow() 가 그대로 읽는 필드명에 맞췄다.
 *   s.sellerId, s.userId, s.businessName, s.representativeName,
 *   s.businessRegistrationNumber, s.contactNumber, s.businessAddress,
 *   s.createdAt, s.status
 */
public record AdminSellerResponse(

        Long sellerId,
        Long userId,
        String businessName,
        String businessRegistrationNumber,
        String representativeName,
        String contactNumber,
        String businessAddress,
        SellerStatus status,
        LocalDateTime createdAt

) {

    public static AdminSellerResponse from(Seller seller) {
        return new AdminSellerResponse(
                seller.getId(),
                seller.getUser().getId(),
                seller.getBusinessName(),
                seller.getBusinessRegistrationNumber(),
                seller.getRepresentativeName(),
                seller.getContactNumber(),
                seller.getBusinessAddress(),
                seller.getStatus(),
                seller.getCreatedAt()
        );
    }
}
