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
 * 관리자 승인 후 실제 발행된 쿠폰
 */
@Getter
@Entity
@Table(
        name = "coupons",
        indexes = {
                @Index(
                        name = "idx_coupon_seller",
                        columnList = "seller_id"
                ),
                @Index(
                        name = "idx_coupon_valid_period",
                        columnList = "valid_from, valid_until"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 발행 요청 원본
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "coupon_request_id",
            nullable = false,
            unique = true
    )
    private CouponRequest couponRequest;

    /**
     * 쿠폰을 발행한 판매자
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

    @Column(
            name = "issued_quantity",
            nullable = false
    )
    private Integer issuedQuantity;

    @Column(
            name = "is_active",
            nullable = false
    )
    private boolean active;

    @Builder
    public Coupon(
            CouponRequest couponRequest,
            SellerApplication seller,
            String couponName,
            CouponDiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minimumOrderAmount,
            BigDecimal maximumDiscountAmount,
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            Integer totalQuantity
    ) {
        this.couponRequest = couponRequest;
        this.seller = seller;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount =
                minimumOrderAmount == null
                        ? BigDecimal.ZERO
                        : minimumOrderAmount;
        this.maximumDiscountAmount = maximumDiscountAmount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.active = true;
    }

    /**
     * 승인된 요청으로 실제 쿠폰을 생성한다.
     */
    public static Coupon from(CouponRequest request) {
        return Coupon.builder()
                .couponRequest(request)
                .seller(request.getSeller())
                .couponName(request.getCouponName())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minimumOrderAmount(
                        request.getMinimumOrderAmount()
                )
                .maximumDiscountAmount(
                        request.getMaximumDiscountAmount()
                )
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .totalQuantity(request.getTotalQuantity())
                .build();
    }

    /**
     * 발행 수량을 한 개 증가시킨다.
     */
    public void issue() {
        if (issuedQuantity >= totalQuantity) {
            throw new IllegalStateException(
                    "쿠폰 발행 수량이 모두 소진되었습니다."
            );
        }

        this.issuedQuantity++;
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * 현재 사용할 수 있는 쿠폰인지 확인한다.
     */
    public boolean isUsable(LocalDateTime now) {
        return active
                && !now.isBefore(validFrom)
                && !now.isAfter(validUntil)
                && issuedQuantity < totalQuantity;
    }
}