package com.shoppingmall.domain.seller.dto.response;

import com.shoppingmall.domain.qna.entity.Qna;

import java.time.LocalDateTime;

/**
 * 판매자 Q&A 답변 응답 DTO
 *
 * (병합 전에는 QnaAnswer 엔티티에서 변환했지만, qna_answers 테이블이
 *  qna 테이블로 병합되면서 이제 Qna 엔티티의 답변 컬럼에서 바로 변환한다.)
 */
public record SellerQnaAnswerResponse(

        Long qnaId,
        String content,
        LocalDateTime answeredAt,
        LocalDateTime answerUpdatedAt

) {

    public static SellerQnaAnswerResponse from(Qna qna) {
        return new SellerQnaAnswerResponse(
                qna.getId(),
                qna.getAnswerContent(),
                qna.getAnsweredAt(),
                qna.getAnswerUpdatedAt()
        );
    }
}