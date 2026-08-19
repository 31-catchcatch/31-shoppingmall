package com.shoppingmall.domain.seller.dto.response;

import java.time.LocalDateTime;

/**
 * 판매자 최종 환불 처리 응답 DTO
 */
public record SellerRefundResponse(

        Long claimId,
        Long orderDetailId,
        Integer refundAmount,
        String refundStatus,
        String memo,
        LocalDateTime refundedAt

) {
}