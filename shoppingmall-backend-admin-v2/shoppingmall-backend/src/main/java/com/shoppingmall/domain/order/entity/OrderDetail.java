package com.shoppingmall.domain.order.entity;

import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductOption;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 하나의 주문에 포함된 개별 상품 정보
 *
 * 판매자별 배송, 구매 확정, 클레임 처리는
 * 전체 Order가 아니라 OrderDetail 단위로 진행한다.
 *
 * NOTE(병합 시 추가): productOption 필드는 원래 sell 버전에는 없었으나,
 * 사이즈/수량 등 옵션이 있는 상품(ProductOption)을 장바구니/주문에서 추적하기 위해
 * merge 과정에서 다시 추가했다. nullable로 두어 옵션이 없는 상품도 주문 가능하다.
 */
@Getter
@Entity
@Table(name = "order_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderDetail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id")
    private ProductOption productOption;

    /**
     * 주문 당시 상품명
     *
     * 주문 이후 상품명이 수정되더라도 주문 내역은 유지하기 위해 저장한다.
     */
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    /**
     * 주문 당시 개별 상품 가격 (옵션 추가금 포함)
     */
    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    /**
     * 해당 주문 상품의 총액
     */
    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 30)
    private DeliveryStatus deliveryStatus;

    @Column(name = "courier_company", length = 50)
    private String courierCompany;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Builder
    public OrderDetail(
            Product product,
            ProductOption productOption,
            Integer unitPrice,
            Integer quantity
    ) {
        this.product = product;
        this.productOption = productOption;
        this.productName = product.getName();
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = unitPrice * quantity;
        this.deliveryStatus = DeliveryStatus.PAYMENT_COMPLETED;
    }

    /**
     * Order.addOrderDetail()에서 호출한다.
     */
    public void assignOrder(Order order) {
        this.order = order;
    }

    /**
     * 판매자가 상품 준비 상태로 변경한다.
     */
    public void prepareDelivery() {
        this.deliveryStatus = DeliveryStatus.PREPARING;
    }

    /**
     * 판매자가 운송장을 입력하고 배송 중 상태로 변경한다.
     */
    public void registerDelivery(
            String courierCompany,
            String trackingNumber
    ) {
        this.courierCompany = courierCompany;
        this.trackingNumber = trackingNumber;
        this.deliveryStatus = DeliveryStatus.SHIPPING;
        this.shippedAt = LocalDateTime.now();
    }

    /**
     * 배송 완료 처리
     */
    public void completeDelivery() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    /**
     * 사용자가 구매 확정한다.
     */
    public void confirmPurchase() {
        this.deliveryStatus = DeliveryStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.deliveryStatus = DeliveryStatus.CANCELED;
    }

    public boolean canRegisterDelivery() {
        return deliveryStatus == DeliveryStatus.PAYMENT_COMPLETED
                || deliveryStatus == DeliveryStatus.PREPARING;
    }
}
