package com.shoppingmall.domain.coupon.entity;

import com.shoppingmall.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DB 정의서 'user_coupons' 테이블 매핑 - 사용자가 보유(발급받은)한 쿠폰.
 * GET /api/v1/users/me/coupons 대응.
 *
 * ⚠ 현재 쿠폰을 사용자 지갑으로 "발급(claim)" 하는 API가 명세서에 없어,
 * 이 엔티티/레포지토리만으로는 실제 데이터가 쌓이지 않는다. 발급 트리거(예: 쿠폰 다운로드 버튼,
 * 회원가입 시 웰컴쿠폰 자동 지급 등) 정책이 확정되면 발급 서비스 로직을 추가해야 한다.
 */
@Getter
@Entity
@Table(
        name = "user_coupons",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_coupon_user_coupon",
                columnNames = {"user_id", "coupon_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "is_used", nullable = false)
    private boolean used;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserCoupon(User user, Coupon coupon) {
        this.user = user;
        this.coupon = coupon;
        this.used = false;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void markAsUsed() {
        this.used = true;
        this.usedAt = LocalDateTime.now();
    }

    /** 주문 전체 환불 시 쿠폰을 다시 사용 가능한 상태로 되돌린다. */
    public void restore() {
        this.used = false;
        this.usedAt = null;
    }
}
