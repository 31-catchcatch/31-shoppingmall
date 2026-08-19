package com.shoppingmall.domain.admin.dto.response;

import com.shoppingmall.domain.inquiry.entity.Inquiry;

import java.time.LocalDateTime;

/**
 * 관리자 - 1:1 문의 목록/단건 응답.
 * 사용자 화면(InquiryResponse)과 달리 "누가" 문의했는지 알아야 하므로 작성자 정보(username/name)를 포함한다.
 */
public record AdminInquiryResponse(
        Long id,
        String username,
        String name,
        String category,
        String orderNumber,
        String title,
        String content,
        String status,
        String answer,
        LocalDateTime answeredAt,
        LocalDateTime createdAt
) {
    public static AdminInquiryResponse from(Inquiry inquiry) {
        return new AdminInquiryResponse(
                inquiry.getId(),
                inquiry.getUser().getUsername(),
                inquiry.getUser().getName(),
                inquiry.getCategory(),
                inquiry.getOrderNumber(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getAnswer(),
                inquiry.getAnsweredAt(),
                inquiry.getCreatedAt()
        );
    }
}
