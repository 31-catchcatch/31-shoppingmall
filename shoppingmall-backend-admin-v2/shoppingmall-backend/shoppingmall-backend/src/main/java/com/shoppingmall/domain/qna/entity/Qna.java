package com.shoppingmall.domain.qna.entity;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 상품 문의 Entity
 *
 * 일반 사용자가 특정 상품에 대해 작성한 질문을 저장한다.
 */
@Getter
@Entity
@Table(name = "qna")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Qna extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 문의를 작성한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    /**
     * 문의 대상 상품
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            nullable = false
    )
    private Product product;

    /**
     * 문의 제목
     */
    @Column(
            nullable = false,
            length = 200
    )
    private String title;

    /**
     * 문의 내용
     */
    @Lob
    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    /**
     * 비공개 문의 여부
     */
    @Column(
            name = "is_secret",
            nullable = false
    )
    private boolean secret;

    /**
     * 판매자 답변 등록 여부
     */
    @Column(
            name = "is_answered",
            nullable = false
    )
    private boolean answered;

    /**
     * 논리 삭제 여부
     */
    @Column(
            name = "is_deleted",
            nullable = false
    )
    private boolean deleted;

    /**
     * 판매자 답변 내용 (DB 정의서 병합: qna_answers.content → qna.answer)
     */
    @Lob
    @Column(
            name = "answer",
            columnDefinition = "TEXT"
    )
    private String answerContent;

    /**
     * 답변자 (판매자 또는 관리자)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answerer_id")
    private User answerer;

    /**
     * 답변 최초 등록일시 (DB 정의서 병합: qna_answers.created_at → qna.answered_at)
     */
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    /**
     * 답변 수정일시 (DB 정의서 병합: qna_answers.updated_at → qna.answer_updated_at)
     */
    @Column(name = "answer_updated_at")
    private LocalDateTime answerUpdatedAt;

    @Builder
    public Qna(
            User user,
            Product product,
            String title,
            String content,
            boolean secret
    ) {
        this.user = user;
        this.product = product;
        this.title = title;
        this.content = content;
        this.secret = secret;
        this.answered = false;
        this.deleted = false;
    }

    /**
     * 판매자 답변을 등록하거나 기존 답변을 수정한다.
     * 최초 등록이면 answered_at 을, 이후 수정이면 answer_updated_at 을 갱신한다.
     */
    public void registerAnswer(String content, User answerer) {
        LocalDateTime now = LocalDateTime.now();

        if (this.answerContent == null) {
            this.answeredAt = now;
            this.answered = true;
        } else {
            this.answerUpdatedAt = now;
        }

        this.answerContent = content;
        this.answerer = answerer;
    }

    /**
     * 질문 내용을 수정한다.
     */
    public void update(
            String title,
            String content,
            boolean secret
    ) {
        this.title = title;
        this.content = content;
        this.secret = secret;
    }

    /**
     * 실제 DB 행을 삭제하지 않고 삭제 상태로 변경한다.
     */
    public void softDelete() {
        this.deleted = true;
    }
}