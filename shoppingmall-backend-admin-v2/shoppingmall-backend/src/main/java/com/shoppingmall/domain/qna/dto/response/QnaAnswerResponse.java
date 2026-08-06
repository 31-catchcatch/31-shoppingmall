package com.shoppingmall.domain.qna.dto.response;

import com.shoppingmall.domain.qna.entity.Qna;

import java.time.LocalDateTime;

/**
 * 상품 문의 답변 응답 DTO
 *
 * (병합 전에는 QnaAnswer 엔티티에서 변환했지만, qna_answers 테이블이
 *  qna 테이블로 병합되면서 이제 Qna 엔티티의 답변 컬럼에서 바로 변환한다.)
 */
public record QnaAnswerResponse(

        Long qnaId,
        String content,
        Long answererId,
        LocalDateTime answeredAt,
        LocalDateTime answerUpdatedAt

) {

    public static QnaAnswerResponse from(Qna qna) {
        return new QnaAnswerResponse(
                qna.getId(),
                qna.getAnswerContent(),
                qna.getAnswerer() == null ? null : qna.getAnswerer().getId(),
                qna.getAnsweredAt(),
                qna.getAnswerUpdatedAt()
        );
    }
}