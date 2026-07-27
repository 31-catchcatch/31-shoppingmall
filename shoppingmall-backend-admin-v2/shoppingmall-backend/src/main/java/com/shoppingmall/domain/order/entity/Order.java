package com.shoppingmall.domain.order.entity;

import com.shoppingmall.domain.coupon.entity.UserCoupon;
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
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30)")
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
     * 이 주문에 실제 적용된 쿠폰.
     *
     * couponDiscountAmount는 할인 "금액"만 저장하므로, 전체 환불 시
     * 어떤 UserCoupon을 다시 사용 가능하게 되돌려야 하는지 알기 위해 별도로 참조를 둔다.
     * 쿠폰을 사용하지 않은 주문은 null이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_coupon_id")
    private UserCoupon usedCoupon;

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
            UserCoupon usedCoupon,
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
        this.usedCoupon = usedCoupon;
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

    /**
     * 이 주문에 부과된 배송비를 돌려준다.
     *
     * 배송비는 별도 컬럼으로 저장하지 않는다. finalPaymentAmount가
     *   totalProductAmount + 배송비 - couponDiscountAmount - usedPointAmount
     * 로 확정되므로, 저장된 네 값에서 아래처럼 정확히 역산되기 때문이다.
     *
     * 정책 상수(무료배송 기준·기본 배송비)로 다시 계산하지 않고 역산하는 이유는,
     * 배송비 정책이 나중에 바뀌어도 과거 주문에는 그 주문이 실제로 부과받은
     * 금액이 그대로 나와야 하기 때문이다.
     *
     * ⚠️ @Column 필드가 아니라 파생 게터다. Order는 필드 접근 전략(@Id가 필드에 있음)이라
     * Hibernate가 이 메서드를 매핑하지 않으므로 테이블 스키마에는 영향이 없다.
     */
    public int getShippingFee() {
        return finalPaymentAmount - totalProductAmount
                + couponDiscountAmount + usedPointAmount;
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

    /**
     * 이 주문에 포함된 모든 주문상세가 환불로 종결되었는지 확인한다.
     * 클레임은 OrderDetail(주문상품) 단위로 처리되므로, 부분 환불과
     * 전체 환불을 구분하기 위해 사용한다.
     */
    public boolean isFullyRefunded() {
        return !orderDetails.isEmpty() && orderDetails.stream()
                .allMatch(detail -> detail.getDeliveryStatus() == DeliveryStatus.REFUNDED);
    }

    /** 주문에 포함된 모든 상품이 환불 완료되었을 때 주문 전체 상태를 환불로 전환한다. */
    public void markRefunded() {
        this.status = OrderStatus.REFUNDED;
    }
}