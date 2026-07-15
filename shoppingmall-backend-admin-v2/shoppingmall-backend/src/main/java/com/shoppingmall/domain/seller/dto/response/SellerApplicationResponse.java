package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;

import java.time.LocalDateTime;

public record SellerApplicationResponse(

        Long applicationId,
        Long userId,
        String businessName,
        String businessRegistrationNumber,
        String representativeName,
        String contactNumber,
        String businessAddress,
        String businessRegistrationFileUrl,
        String mailOrderReportFileUrl,
        SellerApplicationStatus status,
        LocalDateTime createdAt

) {

    public static SellerApplicationResponse from(
            SellerApplication application
    ) {
        return new SellerApplicationResponse(
                application.getId(),
                application.getUser().getId(),
                application.getBusinessName(),
                application.getBusinessRegistrationNumber(),
                application.getRepresentativeName(),
                application.getContactNumber(),
                application.getBusinessAddress(),
                application.getBusinessRegistrationFileUrl(),
                application.getMailOrderReportFileUrl(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }
}