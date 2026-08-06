package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.qna.entity.Qna;

import java.time.LocalDateTime;

/**
 * 판매자용 Q&A 조회 응답 DTO
 */
public record SellerQnaResponse(

        Long qnaId,
        Long productId,
        String productName,
        Long userId,
        String questionTitle,
        String questionContent,
        Boolean answered,
        LocalDateTime createdAt,
        SellerQnaAnswerResponse answer

) {

    /**
     * Qna 엔티티를 판매자용 응답 DTO로 변환한다.
     */
    public static SellerQnaResponse from(Qna qna) {
        return new SellerQnaResponse(
                qna.getId(),
                qna.getProduct().getId(),
                qna.getProduct().getName(),
                qna.getUser().getId(),
                qna.getTitle(),
                qna.getContent(),
                qna.isAnswered(),
                qna.getCreatedAt(),
                qna.isAnswered()
                        ? SellerQnaAnswerResponse.from(qna)
                        : null
        );
    }
}