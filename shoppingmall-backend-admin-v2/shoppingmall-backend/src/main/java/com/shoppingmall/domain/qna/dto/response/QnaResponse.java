package com.shoppingmall.domain.qna.dto.response;

import com.shoppingmall.domain.qna.entity.Qna;

import java.time.LocalDateTime;

/**
 * 상품 문의 단건 응답 DTO.
 *
 * NOTE(병합 시 추가): sell 버전 원본은 secret(비밀글) 여부와 상관없이 항상 전체 내용을 내려줬다.
 * 작성자 본인이 아닌 사람이 비밀글 내용을 볼 수 있으면 안 되므로,
 * 마스킹 처리가 가능한 from(Qna, Long currentUserId) 오버로드를 추가했다.
 * 판매자/관리자용 조회처럼 항상 전체를 봐야 하는 곳은 기존 from(Qna)를 그대로 쓰면 된다.
 */
public record QnaResponse(
        Long qnaId,
        Long productId,
        String productName,
        Long userId,
        String title,
        String content,
        boolean secret,
        boolean answered,
        LocalDateTime createdAt,
        QnaAnswerResponse answer
) {

    /** 마스킹 없이 전체 내용 (판매자/관리자 등 항상 볼 수 있는 주체용) */
    public static QnaResponse from(Qna qna) {
        return new QnaResponse(
                qna.getId(),
                qna.getProduct().getId(),
                qna.getProduct().getName(),
                qna.getUser().getId(),
                qna.getTitle(),
                qna.getContent(),
                qna.isSecret(),
                qna.isAnswered(),
                qna.getCreatedAt(),
                qna.getAnswer() == null ? null : QnaAnswerResponse.from(qna.getAnswer())
        );
    }

    /** 일반 사용자 조회용 - 비밀글이고 작성자 본인이 아니면 제목/내용/답변을 마스킹 */
    public static QnaResponse from(Qna qna, Long currentUserId) {
        boolean isOwner = currentUserId != null && qna.getUser().getId().equals(currentUserId);
        boolean shouldMask = qna.isSecret() && !isOwner;

        String displayTitle = shouldMask ? "비밀글입니다." : qna.getTitle();
        String displayContent = shouldMask ? "작성자와 판매자만 볼 수 있습니다." : qna.getContent();
        QnaAnswerResponse displayAnswer = (qna.getAnswer() == null)
                ? null
                : (shouldMask ? null : QnaAnswerResponse.from(qna.getAnswer()));

        return new QnaResponse(
                qna.getId(),
                qna.getProduct().getId(),
                qna.getProduct().getName(),
                qna.getUser().getId(),
                displayTitle,
                displayContent,
                qna.isSecret(),
                qna.isAnswered(),
                qna.getCreatedAt(),
                displayAnswer
        );
    }
}
