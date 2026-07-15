package com.shoppingmall.domain.qna.dto.response;

import com.shoppingmall.domain.qna.entity.QnaAnswer;

import java.time.LocalDateTime;

/**
 * 상품 문의 답변 응답 DTO
 */
public record QnaAnswerResponse(

        Long answerId,
        Long qnaId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static QnaAnswerResponse from(
            QnaAnswer answer
    ) {
        return new QnaAnswerResponse(
                answer.getId(),
                answer.getQna().getId(),
                answer.getContent(),
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}