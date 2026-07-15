package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.qna.dto.response.QnaResponse;
import com.shoppingmall.domain.qna.repository.QnaRepository;
import com.shoppingmall.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API 명세서 "관리자 - 운영 - 전체 Q&A 모니터링 조회" (GET /admin/qna) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQnaService {

    private final QnaRepository qnaRepository;

    public PageResponse<QnaResponse> getAllQna(Pageable pageable) {
        // 관리자는 비밀글이어도 전체 내용을 봐야 하므로 마스킹 없는 from(Qna) 사용
        Page<QnaResponse> page = qnaRepository.findAll(pageable).map(QnaResponse::from);
        return PageResponse.from(page);
    }
}
