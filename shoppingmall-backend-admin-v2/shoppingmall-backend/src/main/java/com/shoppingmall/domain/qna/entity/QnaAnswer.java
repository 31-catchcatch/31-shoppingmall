package com.shoppingmall.domain.qna.entity;

import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 판매자의 상품 문의 답변 Entity
 */
@Getter
@Entity
@Table(name = "qna_answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 답변 대상 질문
     *
     * QnaAnswer가 qna_id 외래키를 관리하는 관계의 주인이다.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "qna_id",
            nullable = false,
            unique = true
    )
    private Qna qna;

    /**
     * 판매자 답변 내용
     */
    @Lob
    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    @Builder
    public QnaAnswer(
            Qna qna,
            String content
    ) {
        this.qna = qna;
        this.content = content;
    }

    /**
     * Qna.registerAnswer()에서 양방향 관계를 설정할 때 사용한다.
     */
    public void assignQna(Qna qna) {
        this.qna = qna;
    }

    /**
     * 기존 답변 내용을 수정한다.
     */
    public void updateContent(String content) {
        this.content = content;
    }
}