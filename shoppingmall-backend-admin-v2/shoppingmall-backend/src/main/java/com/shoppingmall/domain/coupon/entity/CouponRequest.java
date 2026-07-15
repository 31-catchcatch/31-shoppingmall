package com.shoppingmall.domain.coupon.entity;

import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 판매자가 관리자에게 제출한 쿠폰 발행 승인 요청
 */
@Getter
@Entity
@Table(
        name = "coupon_requests",
        indexes = {
                @Index(
                        name = "idx_coupon_request_seller",
                        columnList = "seller_id"
                ),
                @Index(
                        name = "idx_coupon_request_status",
                        columnList = "status"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 쿠폰 발행을 요청한 판매자
     *
     * 현재 Product가 SellerApplication을 판매자로 사용하고 있으므로
     * 동일한 구조로 맞춘다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "seller_id",
            nullable = false
    )
    private SellerApplication seller;

    @Column(
            name = "coupon_name",
            nullable = false,
            length = 100
    )
    private String couponName;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "discount_type",
            nullable = false,
            length = 30
    )
    private CouponDiscountType discountType;

    /**
     * 정액 할인일 경우 금액,
     * 정률 할인일 경우 할인율을 저장한다.
     */
    @Column(
            name = "discount_value",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal discountValue;

    @Column(
            name = "minimum_order_amount",
            precision = 15,
            scale = 2
    )
    private BigDecimal minimumOrderAmount;

    @Column(
            name = "maximum_discount_amount",
            precision = 15,
            scale = 2
    )
    private BigDecimal maximumDiscountAmount;

    @Column(
            name = "valid_from",
            nullable = false
    )
    private LocalDateTime validFrom;

    @Column(
            name = "valid_until",
            nullable = false
    )
    private LocalDateTime validUntil;

    @Column(
            name = "total_quantity",
            nullable = false
    )
    private Integer totalQuantity;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private CouponRequestStatus status;

    /**
     * 관리자가 반려한 사유
     */
    @Column(
            name = "rejection_reason",
            length = 1000
    )
    private String rejectionReason;

    /**
     * 관리자가 요청을 처리한 시각
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    public CouponRequest(
            SellerApplication seller,
            String couponName,
            String discountType,
            BigDecimal discountValue,
            BigDecimal minimumOrderAmount,
            BigDecimal maximumDiscountAmount,
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            Integer totalQuantity,
            CouponRequestStatus status
    ) {
        this.seller = seller;
        this.couponName = couponName;
        this.discountType = CouponDiscountType.valueOf(
                discountType.trim().toUpperCase()
        );
        this.discountValue = discountValue;
        this.minimumOrderAmount =
                minimumOrderAmount == null
                        ? BigDecimal.ZERO
                        : minimumOrderAmount;
        this.maximumDiscountAmount = maximumDiscountAmount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.totalQuantity = totalQuantity;
        this.status = status == null
                ? CouponRequestStatus.PENDING
                : status;
    }

    /**
     * 관리자가 쿠폰 발행 요청을 승인한다.
     */
    public void approve() {
        this.status = CouponRequestStatus.APPROVED;
        this.rejectionReason = null;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 관리자가 쿠폰 발행 요청을 반려한다.
     */
    public void reject(String rejectionReason) {
        this.status = CouponRequestStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * 판매자가 승인 대기 중인 요청을 취소한다.
     */
    public void cancel() {
        this.status = CouponRequestStatus.CANCELED;
    }
}