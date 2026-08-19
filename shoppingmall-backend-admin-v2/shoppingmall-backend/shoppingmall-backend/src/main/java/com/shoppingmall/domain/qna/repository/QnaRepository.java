package com.shoppingmall.domain.qna.repository;

import com.shoppingmall.domain.qna.entity.Qna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 상품 문의 Repository
 */
public interface QnaRepository extends JpaRepository<Qna, Long> {

    /**
     * 삭제되지 않은 문의 단건 조회
     */
    Optional<Qna> findByIdAndDeletedFalse(Long qnaId);

    /**
     * 일반 사용자용 특정 상품 문의 목록 조회
     */
    Page<Qna> findAllByProduct_IdAndDeletedFalseOrderByCreatedAtDesc(
            Long productId,
            Pageable pageable
    );

    /**
     * 특정 판매자의 전체 상품 문의 조회
     */
    Page<Qna>
    findAllByProduct_Seller_IdAndDeletedFalseOrderByCreatedAtDesc(
            Long sellerId,
            Pageable pageable
    );

    /**
     * 특정 판매자의 특정 상품 문의 조회
     */
    Page<Qna>
    findAllByProduct_Seller_IdAndProduct_IdAndDeletedFalseOrderByCreatedAtDesc(
            Long sellerId,
            Long productId,
            Pageable pageable
    );

    /**
     * 특정 판매자의 답변 여부별 문의 조회
     */
    Page<Qna>
    findAllByProduct_Seller_IdAndAnsweredAndDeletedFalseOrderByCreatedAtDesc(
            Long sellerId,
            Boolean answered,
            Pageable pageable
    );

    /**
     * 특정 판매자의 상품 ID 및 답변 여부 조건 조회
     */
    Page<Qna>
    findAllByProduct_Seller_IdAndProduct_IdAndAnsweredAndDeletedFalseOrderByCreatedAtDesc(
            Long sellerId,
            Long productId,
            Boolean answered,
            Pageable pageable
    );

    /**
     * 판매자 대시보드용 미답변 문의 수
     */
    long countByProduct_Seller_IdAndAnsweredFalseAndDeletedFalse(
            Long sellerId
    );
}