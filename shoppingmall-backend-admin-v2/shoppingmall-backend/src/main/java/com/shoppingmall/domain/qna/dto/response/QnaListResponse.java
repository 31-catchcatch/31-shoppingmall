package com.shoppingmall.domain.qna.dto.response;

import com.shoppingmall.domain.qna.entity.Qna;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 상품 문의 목록 응답 DTO
 */
public record QnaListResponse(

        List<QnaResponse> qnaList,
        int page,
        int size,
        long totalElements,
        int totalPages

) {

    public static QnaListResponse from(
            Page<Qna> qnaPage
    ) {
        return new QnaListResponse(
                qnaPage.getContent()
                        .stream()
                        .map(QnaResponse::from)
                        .toList(),
                qnaPage.getNumber(),
                qnaPage.getSize(),
                qnaPage.getTotalElements(),
                qnaPage.getTotalPages()
        );
    }
}