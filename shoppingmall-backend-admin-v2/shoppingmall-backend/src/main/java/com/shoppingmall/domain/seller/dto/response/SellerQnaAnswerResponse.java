package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.qna.entity.QnaAnswer;

import java.time.LocalDateTime;

/**
 * 판매자 Q&A 답변 응답 DTO
 */
public record SellerQnaAnswerResponse(

        Long answerId,
        Long qnaId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    /**
     * QnaAnswer 엔티티를 응답 DTO로 변환한다.
     */
    public static SellerQnaAnswerResponse from(
            QnaAnswer answer
    ) {
        return new SellerQnaAnswerResponse(
                answer.getId(),
                answer.getQna().getId(),
                answer.getContent(),
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}