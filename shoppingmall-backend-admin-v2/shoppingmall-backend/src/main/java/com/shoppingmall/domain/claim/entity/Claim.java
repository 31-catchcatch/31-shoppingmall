package com.shoppingmall.domain.claim.entity;

import com.shoppingmall.domain.order.entity.OrderDetail;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 교환 / 환불 신청 Entity
 */
@Getter
@Entity
@Table(name = "claims")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Claim extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문 상세
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id")
    private OrderDetail orderDetail;

    /**
     * 교환 / 환불
     */
    @Enumerated(EnumType.STRING)
    private ClaimType type;

    /**
     * 신청 사유
     */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /**
     * 판매자 처리 사유
     */
    @Column(columnDefinition = "TEXT")
    private String processReason;

    /**
     * 현재 상태
     */
    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    /**
     * 환불 금액
     */
    private Integer claimAmount;

    /**
     * 처리 시간
     */
    private LocalDateTime processedAt;

    @Builder
    public Claim(
            OrderDetail orderDetail,
            ClaimType type,
            String reason,
            Integer claimAmount
    ) {
        this.orderDetail = orderDetail;
        this.type = type;
        this.reason = reason;
        this.claimAmount = claimAmount;
        this.status = ClaimStatus.REQUESTED;
    }

    public void accept(String reason) {
        this.status = ClaimStatus.ACCEPTED;
        this.processReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = ClaimStatus.REJECTED;
        this.processReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public void startProcessing(String reason) {
        this.status = ClaimStatus.PROCESSING;
        this.processReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ClaimStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }
}