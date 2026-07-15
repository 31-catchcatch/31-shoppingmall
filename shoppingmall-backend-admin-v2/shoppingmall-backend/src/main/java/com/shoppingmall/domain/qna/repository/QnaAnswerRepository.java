package com.shoppingmall.domain.qna.repository;

import com.shoppingmall.domain.qna.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 상품 문의 답변 Repository
 */
public interface QnaAnswerRepository
        extends JpaRepository<QnaAnswer, Long> {

    /**
     * Q&A ID로 답변 조회
     */
    Optional<QnaAnswer> findByQna_Id(Long qnaId);

    /**
     * 특정 질문에 답변이 있는지 확인
     */
    boolean existsByQna_Id(Long qnaId);
}