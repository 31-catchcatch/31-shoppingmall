package com.shoppingmall.domain.order.entity;

import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자의 전체 주문 정보를 저장하는 Entity
 */
@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문번호
     *
     * 외부에 DB PK 대신 노출할 수 있는 별도 주문번호
     */
    @Column(
            name = "order_number",
            nullable = false,
            unique = true,
            length = 50
    )
    private String orderNumber;

    /**
     * 주문한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    /**
     * 할인 전 상품 총액
     */
    @Column(name = "total_product_amount", nullable = false)
    private Integer totalProductAmount;

    /**
     * 쿠폰 할인 금액
     */
    @Column(name = "coupon_discount_amount", nullable = false)
    private Integer couponDiscountAmount;

    /**
     * 포인트 사용 금액
     */
    @Column(name = "used_point_amount", nullable = false)
    private Integer usedPointAmount;

    /**
     * 최종 결제 금액
     */
    @Column(name = "final_payment_amount", nullable = false)
    private Integer finalPaymentAmount;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "delivery_request", length = 255)
    private String deliveryRequest;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderDetail> orderDetails = new ArrayList<>();

    @Builder
    public Order(
            String orderNumber,
            User user,
            Integer totalProductAmount,
            Integer couponDiscountAmount,
            Integer usedPointAmount,
            Integer finalPaymentAmount,
            String receiverName,
            String receiverPhone,
            String zipCode,
            String address,
            String addressDetail,
            String deliveryRequest
    ) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.status = OrderStatus.PENDING;
        this.totalProductAmount = totalProductAmount;
        this.couponDiscountAmount =
                couponDiscountAmount == null ? 0 : couponDiscountAmount;
        this.usedPointAmount =
                usedPointAmount == null ? 0 : usedPointAmount;
        this.finalPaymentAmount = finalPaymentAmount;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.deliveryRequest = deliveryRequest;
    }

    /**
     * 주문과 주문 상세의 양방향 관계를 설정한다.
     */
    public void addOrderDetail(OrderDetail orderDetail) {
        this.orderDetails.add(orderDetail);
        orderDetail.assignOrder(this);
    }

    public void completePayment() {
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELED;
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }
}