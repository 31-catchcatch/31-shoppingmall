package com.shoppingmall.domain.point.entity;

import com.shoppingmall.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DB 정의서 'point_histories' 테이블.
 * 포인트가 바뀔 때마다(적립/사용/관리자 조정) 한 행씩 쌓이는 이력 테이블.
 * amount 는 양수(적립/조정 증가)/음수(사용/조정 감소) 모두 가능.
 */
@Getter
@Entity
@Table(name = "point_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer amount; // +1000, -500 등

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter; // 이 변동 반영 후 잔액 (조회 화면에 바로 쓰기 위한 스냅샷)

    @Column(nullable = false, length = 200)
    private String reason; // 예: "구매 확정 적립", "리뷰 작성 적립", "관리자 수동 조정"

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PointHistory(User user, Integer amount, Integer balanceAfter, String reason) {
        this.user = user;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
