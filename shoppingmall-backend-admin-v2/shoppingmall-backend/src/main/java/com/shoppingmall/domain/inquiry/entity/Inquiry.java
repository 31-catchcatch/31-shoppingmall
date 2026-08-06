package com.shoppingmall.domain.inquiry.entity;

import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 고객센터 1:1 문의. POST /api/v1/customer-center/inquiries 대응 (프론트 customercenter.js).
 * 상품 Q&A(qna)와 달리 특정 상품에 묶이지 않는 플랫폼 문의라 별도 도메인으로 분리한다.
 */
@Getter
@Entity
@Table(name = "customer_inquiries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 프론트 카테고리 값 그대로 저장: order / delivery / cancel / member / etc */
    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "order_number", length = 50)
    private String orderNumber; // 주문 관련 문의 시 선택 입력

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20)")
    private InquiryStatus status;

    @Column(columnDefinition = "TEXT")
    private String answer; // 관리자 답변 (추후 관리자 화면에서 사용)

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Builder
    public Inquiry(User user, String category, String orderNumber, String title, String content) {
        this.user = user;
        this.category = category;
        this.orderNumber = orderNumber;
        this.title = title;
        this.content = content;
        this.status = InquiryStatus.RECEIVED;
    }

    public void answer(String answer) {
        this.answer = answer;
        this.status = InquiryStatus.ANSWERED;
        this.answeredAt = LocalDateTime.now();
    }
}
