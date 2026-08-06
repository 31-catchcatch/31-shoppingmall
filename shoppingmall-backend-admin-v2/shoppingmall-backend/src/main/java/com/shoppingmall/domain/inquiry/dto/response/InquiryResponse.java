package com.shoppingmall.domain.inquiry.dto.response;

import com.shoppingmall.domain.inquiry.entity.Inquiry;

import java.time.LocalDateTime;

/** 고객센터 문의 단건/목록 응답 */
public record InquiryResponse(
        Long id,
        String category,
        String orderNumber,
        String title,
        String content,
        String status,
        String answer,
        LocalDateTime answeredAt,
        LocalDateTime createdAt
) {
    public static InquiryResponse from(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
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
