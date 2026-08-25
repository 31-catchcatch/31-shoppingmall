package com.shoppingmall.domain.order.service;

import com.shoppingmall.domain.cart.repository.CartItemRepository;
import com.shoppingmall.domain.coupon.entity.Coupon;
import com.shoppingmall.domain.coupon.entity.CouponDiscountType;
import com.shoppingmall.domain.coupon.entity.UserCoupon;
import com.shoppingmall.domain.coupon.repository.UserCouponRepository;
import com.shoppingmall.domain.order.dto.request.OrderCreateRequest;
import com.shoppingmall.domain.order.dto.response.CheckoutResponse;
import com.shoppingmall.domain.order.dto.response.OrderDeliveryResponse;
import com.shoppingmall.domain.order.dto.response.OrderListResponse;
import com.shoppingmall.domain.order.dto.response.OrderReceiptResponse;
import com.shoppingmall.domain.order.dto.response.OrderResponse;
import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderDetail;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.repository.OrderDetailRepository;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.payment.entity.Payment;
import com.shoppingmall.domain.payment.repository.PaymentRepository;
import com.shoppingmall.domain.point.service.PointService;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductOption;
import com.shoppingmall.domain.product.repository.ProductOptionRepository;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.settlement.entity.Settlement;
import com.shoppingmall.domain.settlement.repository.SettlementRepository;
import com.shoppingmall.domain.user.entity.Address;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.AddressRepository;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.domain.cart.entity.CartItem;
import com.shoppingmall.domain.order.dto.request.OrderPrepareRequest;
import com.shoppingmall.domain.order.dto.response.OrderDraftResponse;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 일반 사용자 주문 관련 서비스.
 *
 * kim이 만든 체크아웃/주문생성 흐름 + sell이 만든 주문내역/구매확정 흐름을
 * sell 버전 Order/OrderDetail 엔티티 기준으로 하나로 합친 버전이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    /** 구매확정 시 적립 포인트 비율(%). 구매액(실결제 기준)의 1%. */
    private static final int POINT_EARN_RATE_PERCENT = 1;

    /** 기본 배송비(원). 상품 총액이 무료배송 기준 미만일 때 부과한다. */
    public static final int SHIPPING_FEE = 3000;

    /** 무료배송 기준 금액(원). 상품 총액이 이 값 이상이면 배송비가 0원이다. */
    public static final int FREE_SHIPPING_THRESHOLD = 50000;

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserCouponRepository userCouponRepository;
    private final SettlementRepository settlementRepository;
    private final PointService pointService;
    private final OrderDraftStore orderDraftStore;
    /** 1. 주문서 진입 데이터 조회 (기본 배송지/보유 포인트) */
    public CheckoutResponse getCheckoutData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Address defaultAddr = addressRepository.findByUserAndDefaultAddressTrue(user).orElse(null);

        return CheckoutResponse.builder()
                .defaultRecipientName(defaultAddr != null ? defaultAddr.getRecipientName() : user.getName())
                .defaultRecipientPhone(defaultAddr != null ? defaultAddr.getRecipientPhone() : user.getPhoneNumber())
                .defaultAddress(defaultAddr != null ? defaultAddr.getBaseAddress() : "")
                .defaultAddressDetail(defaultAddr != null ? defaultAddr.getDetailAddress() : "")
                .availablePoint(user.getPoint())
                .shippingFee(SHIPPING_FEE)
                .freeShippingThreshold(FREE_SHIPPING_THRESHOLD)
                .build();
    }

    /**
     * [1-3 조치] 주문서 진입 — 서버가 주문 대상과 금액을 확정한다.
     *
     * <p>바로구매(items)와 장바구니 주문(cartItemIds) 두 경로를 동일한 초안으로 만든다.
     * 이후 결제 요청에는 draftId 만 실리므로 상품·옵션·수량을 바꿔 보낼 방법이 없다.
     *
     * <p>재고는 여기서 차감하지 않는다. 주문서만 열고 이탈하는 경우가 많아 묶이기 때문이다.
     */
    public OrderDraftResponse prepareOrder(Long userId, OrderPrepareRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<OrderDraftStore.DraftItem> draftItems = new ArrayList<>();
        List<OrderDraftResponse.Item> viewItems = new ArrayList<>();
        int totalProductAmount = 0;

        record Target(Long productId, Long optionId, int quantity) {}
        List<Target> targets = new ArrayList<>();

        if (request.cartItemIds() != null && !request.cartItemIds().isEmpty()) {
            // 장바구니 경로 — 본인 장바구니에 실제로 있는 항목만 인정한다.
            Map<Long, CartItem> mine = cartItemRepository.findAllByUser(user).stream()
                    .collect(Collectors.toMap(CartItem::getId, ci -> ci));
            for (Long cartItemId : request.cartItemIds()) {
                CartItem ci = mine.get(cartItemId);
                if (ci == null) {
                    throw new CustomException(ErrorCode.INVALID_INPUT);
                }
                targets.add(new Target(ci.getProduct().getId(),
                        ci.getProductOption().getId(), ci.getQuantity()));
            }
        } else if (request.items() != null && !request.items().isEmpty()) {
            for (OrderPrepareRequest.DirectItem it : request.items()) {
                targets.add(new Target(it.productId(), it.optionId(), it.quantity()));
            }
        } else {
            throw new CustomException(ErrorCode.ORDER_ITEM_REQUIRED);
        }

        for (Target t : targets) {
            Product product = productRepository.findByIdAndDeletedFalse(t.productId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            if (!product.isOnSale()) {
                throw new CustomException(ErrorCode.PRODUCT_NOT_ON_SALE);
            }

            ProductOption option = product.getActiveOptions().stream()
                    .filter(o -> o.getId().equals(t.optionId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

            int unitPrice = product.getPrice() + option.getAdditionalPrice();
            int lineAmount = unitPrice * t.quantity();
            totalProductAmount += lineAmount;

            draftItems.add(new OrderDraftStore.DraftItem(
                    product.getId(), option.getId(), t.quantity(), unitPrice));
            viewItems.add(new OrderDraftResponse.Item(
                    product.getId(), option.getId(), product.getName(), option.getOptionName(),
                    product.getThumbnailUrl(), unitPrice, t.quantity(), lineAmount));
        }

        int shippingFee = calcShippingFee(totalProductAmount);
        String draftId = orderDraftStore.put(new OrderDraftStore.Draft(
                userId, draftItems, totalProductAmount, shippingFee));

        return new OrderDraftResponse(draftId, viewItems,
                totalProductAmount, shippingFee, totalProductAmount + shippingFee);
    }

    /** 주문서 새로고침용 — 초안을 소모하지 않고 조회만 한다. */
    public OrderDraftResponse getDraft(Long userId, String draftId) {
        OrderDraftStore.Draft draft = orderDraftStore.peek(draftId, userId);

        List<OrderDraftResponse.Item> viewItems = new ArrayList<>();
        for (OrderDraftStore.DraftItem d : draft.items()) {
            Product product = productRepository.findByIdAndDeletedFalse(d.productId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
            String optionName = product.getActiveOptions().stream()
                    .filter(o -> o.getId().equals(d.optionId()))
                    .map(ProductOption::getOptionName)
                    .findFirst().orElse("");
            viewItems.add(new OrderDraftResponse.Item(
                    d.productId(), d.optionId(), product.getName(), optionName,
                    product.getThumbnailUrl(), d.unitPrice(), d.quantity(),
                    d.unitPrice() * d.quantity()));
        }
        return new OrderDraftResponse(draftId, viewItems,
                draft.totalProductAmount(), draft.shippingFee(),
                draft.totalProductAmount() + draft.shippingFee());
    }

    /** 2. 최종 주문 생성 (재고 확인 + 주문/주문상세 영속화 + 장바구니에서 제거) */
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int usePoint = request.usePoint() == null ? 0 : request.usePoint();
        if (usePoint > user.getPoint()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // [1-3 조치] 주문 대상은 요청에서 받지 않는다. 주문서 진입 시 서버가 확정한 초안을 꺼내 쓴다.
        //            consume 은 조회와 동시에 제거하므로 같은 초안으로 두 번 주문할 수 없다.
        OrderDraftStore.Draft draft = orderDraftStore.consume(request.draftId(), userId);

        List<OrderDetail> details = new ArrayList<>();
        int totalProductAmount = 0;

        for (OrderDraftStore.DraftItem item : draft.items()) {
            Product product = productRepository.findByIdAndDeletedFalse(item.productId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            // 초안 발급 후 판매중지되었을 수 있으므로 확정 시점에 다시 본다.
            if (!product.isOnSale()) {
                throw new CustomException(ErrorCode.PRODUCT_NOT_ON_SALE);
            }

            ProductOption option = product.getActiveOptions().stream()
                    .filter(o -> o.getId().equals(item.optionId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

            int decreased = productOptionRepository.decreaseStock(option.getId(), item.quantity());
            if (decreased == 0) {
                throw new CustomException(ErrorCode.OUT_OF_STOCK);
            }

            // 단가는 초안에 고정된 값을 쓴다. 주문서 표시 금액과 청구 금액이 어긋나지 않는다.
            int unitPrice = item.unitPrice();

            details.add(OrderDetail.builder()
                    .product(product)
                    .productOption(option)
                    .unitPrice(unitPrice)
                    .quantity(item.quantity())
                    .build());

            totalProductAmount += unitPrice * item.quantity();

            cartItemRepository.findByUserAndProductIdAndProductOptionId(
                            user, product.getId(), option.getId())
                    .ifPresent(cartItemRepository::delete);
        }

        // 쿠폰 할인 적용: couponId가 오면 보유(UserCoupon) 검증 후 할인액 계산 및 사용 처리
        int couponDiscountAmount = 0;
        UserCoupon appliedCoupon = null;
        if (request.couponId() != null) {
            UserCoupon userCoupon = userCouponRepository
                    .findByUser_IdAndCoupon_IdAndUsedFalse(userId, request.couponId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_AVAILABLE));

            Coupon coupon = userCoupon.getCoupon();
            LocalDateTime now = LocalDateTime.now();
            if (!coupon.isActive() || now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
                throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
            }
            if (BigDecimal.valueOf(totalProductAmount).compareTo(coupon.getMinimumOrderAmount()) < 0) {
                throw new CustomException(ErrorCode.COUPON_MINIMUM_NOT_MET);
            }

            BigDecimal discount;
            if (coupon.getDiscountType() == CouponDiscountType.FIXED_AMOUNT) {
                discount = coupon.getDiscountValue();
            } else { // PERCENTAGE
                discount = BigDecimal.valueOf(totalProductAmount)
                        .multiply(coupon.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
                if (coupon.getMaximumDiscountAmount() != null
                        && discount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
                    discount = coupon.getMaximumDiscountAmount();
                }
            }
            couponDiscountAmount = Math.min(discount.intValue(), totalProductAmount);
            userCoupon.markAsUsed();
            appliedCoupon = userCoupon;
        }

        // 배송비는 쿠폰 할인 대상이 아니므로 상품 총액 기준으로 계산해 그대로 더한다.
        int shippingFee = calcShippingFee(totalProductAmount);

        int finalPaymentAmount = totalProductAmount + shippingFee - couponDiscountAmount - usePoint;
        if (finalPaymentAmount < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .totalProductAmount(totalProductAmount)
                .couponDiscountAmount(couponDiscountAmount)
                .usedCoupon(appliedCoupon)
                .usedPointAmount(usePoint)
                .finalPaymentAmount(finalPaymentAmount)
                .receiverName(request.receiverName())
                .receiverPhone(request.receiverPhone())
                .zipCode(request.zipCode())
                .address(request.address())
                .addressDetail(request.addressDetail())
                .deliveryRequest(request.deliveryRequest())
                .build();

        details.forEach(order::addOrderDetail);

        Order saved = orderRepository.save(order);

        // 포인트 사용분을 실제 잔액에서 차감한다 (이전엔 결제 금액 계산에만 반영되고
        // User.point 잔액은 그대로여서 같은 포인트를 무한히 재사용할 수 있었다).
        if (usePoint > 0) {
            pointService.adjustPoint(
                    userId,
                    -usePoint,
                    "주문 결제 시 포인트 사용 (주문번호: " + saved.getOrderNumber() + ")"
            );
        }

        return OrderResponse.from(saved);
    }

    /**
     * 2-1. 결제 대기 주문 취소 — 결제창을 닫거나 결제에 실패했을 때 되돌리기.
     *
     * placeOrder()는 결제 여부와 무관하게 주문을 만드는 시점에 재고·쿠폰·포인트를 먼저
     * 소모한다. 결제가 성사되지 않으면 그 자원이 아무도 쓸 수 없는 채로 잠기기 때문에
     * 이 메서드가 셋을 되돌린다. 되돌리는 방식은 환불 경로(SellerRefundService)와 같다.
     *
     * 적립 포인트는 회수 대상이 아니다. 적립은 결제가 아니라 구매확정 시점에 지급되므로
     * (confirmPurchase 참고) 결제 전 단계에는 회수할 적립분이 존재하지 않는다.
     *
     * PENDING 주문만 취소할 수 있다. 결제가 끝난 주문의 취소는 PG 승인까지 되돌려야 해서
     * 여기서 다루지 않는다.
     *
     * 장바구니는 복구하지 않는다 (주문 생성 시 삭제된다). 재구매하려면 다시 담아야 한다.
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        // 이미 취소된 주문이면 아무것도 되돌리지 않고 현재 상태만 돌려준다.
        // 프론트가 결제창 취소와 실패 랜딩 양쪽에서 호출할 수 있어 중복 호출이 정상 경로이고,
        // 여기서 복구를 재실행하면 재고와 포인트가 이중으로 늘어난다.
        if (order.getStatus() == OrderStatus.CANCELED) {
            return OrderResponse.from(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomException(ErrorCode.ORDER_CANCEL_NOT_ALLOWED);
        }

        // 1) 재고 복구. 옵션이 없는 주문상품은 애초에 차감되지 않았으므로 되돌릴 것도 없다.
        for (OrderDetail detail : order.getOrderDetails()) {
            if (detail.getProductOption() != null) {
                productOptionRepository.restoreStock(
                        detail.getProductOption().getId(),
                        detail.getQuantity()
                );
            }
            detail.cancel();
        }

        // 2) 쿠폰을 다시 사용 가능한 상태로 되돌린다.
        if (order.getUsedCoupon() != null) {
            order.getUsedCoupon().restore();
        }

        // 3) 사용한 포인트를 잔액으로 되돌린다.
        if (order.getUsedPointAmount() > 0) {
            pointService.adjustPoint(
                    userId,
                    order.getUsedPointAmount(),
                    "주문 취소로 인한 포인트 복원 (주문번호: " + order.getOrderNumber() + ")"
            );
        }

        // 4) 결제 시도 원장이 남아 있으면 함께 닫는다. READY 로 두면 승인 대기처럼 보인다.
        paymentRepository.findByOrder_Id(order.getId()).ifPresent(Payment::cancelPayment);

        order.cancel();

        // 취소 사유를 담을 컬럼이 없어 로그로만 남긴다 (DB 스키마 무변경).
        log.info("주문 취소: orderNumber={}, userId={}, reason={}", order.getOrderNumber(), userId, reason);

        return OrderResponse.from(order);
    }

    /** 3. 내 주문 내역 조회 (상태 필터 선택) */
    public OrderListResponse getMyOrders(Long userId, OrderStatus status, int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(normalizedPage, normalizedSize);

        Page<Order> orders = (status == null)
                ? orderRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, pageable)
                : orderRepository.findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId, status, pageable);

        return OrderListResponse.from(orders);
    }

    /** 4. 내 주문 단건 조회 */
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        return OrderResponse.from(order);
    }

    /** 5. 구매 확정 (배송 완료 상태에서만 가능) */
    @Transactional
    public void confirmPurchase(Long userId, Long orderDetailId) {
        OrderDetail detail = orderDetailRepository.findByIdAndOrder_User_Id(orderDetailId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if (detail.getDeliveryStatus() != DeliveryStatus.DELIVERED) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        detail.confirmPurchase();

        // 구매확정 시점에 정산 데이터를 자동 생성한다 (이전엔 이 생성 로직이 없어 정산 내역이 항상 비어있었음).
        // 같은 주문상세로 중복 생성되지 않도록 방어(existsByOrderDetail_Id) - confirmPurchase는 원래
        // DELIVERED 상태에서만 호출 가능해 재호출 여지가 거의 없지만 안전하게 체크한다.
        if (!settlementRepository.existsByOrderDetail_Id(detail.getId())) {
            settlementRepository.save(Settlement.builder()
                    .seller(detail.getProduct().getSeller())
                    .orderDetail(detail)
                    .saleAmount(detail.getTotalPrice())
                    .feeRate(Settlement.DEFAULT_FEE_RATE)
                    .build());
        }

        // 구매확정 시 구매액(실결제 기준)의 1%를 포인트로 적립한다.
        // 쿠폰/포인트 할인은 주문 전체 단위이므로, 이 주문상품이 차지하는 정가 비중만큼
        // 실결제액을 배분해 적립 기준으로 삼는다.
        //
        // 단, 배송비는 적립 대상에서 제외한다. 배송비는 매출이 아니라 원가를 그대로
        // 전가한 금액이라 여기에 적립을 주면 배송비를 낼수록 이득이 되기 때문이다.
        // 그래서 finalPaymentAmount에서 배송비를 뺀 값(= 상품 실결제액)을 기준으로 쓴다.
        // 주문의 모든 상품을 확정하면 적립 합계 = (finalPaymentAmount - 배송비) × 1% 가 된다.
        // (구매확정 후에는 클레임/환불이 불가능하므로 적립 회수 로직은 필요 없다.)
        Order order = detail.getOrder();
        int totalProductAmount = order.getTotalProductAmount();
        if (totalProductAmount > 0) {
            int productPaymentAmount =
                    order.getFinalPaymentAmount() - order.getShippingFee();
            long earnPoint = (long) productPaymentAmount * detail.getTotalPrice()
                    * POINT_EARN_RATE_PERCENT / ((long) totalProductAmount * 100);
            if (earnPoint > 0) {
                pointService.adjustPoint(
                        userId,
                        (int) earnPoint,
                        "구매확정 적립 (주문번호: " + order.getOrderNumber() + ")"
                );
            }
        }
    }

    /** 6. 배송 상태 조회 - 주문에 포함된 주문상세 항목별 배송 추적 정보 */
    public List<OrderDeliveryResponse> getDeliveryInfo(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        return order.getOrderDetails().stream()
                .map(OrderDeliveryResponse::from)
                .toList();
    }

    /** 7. 영수증 조회 - PG 결제 내역(Payment) 기반 전자 영수증 */
    public OrderReceiptResponse getReceipt(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        Payment payment = paymentRepository.findByOrder_Id(order.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

        return OrderReceiptResponse.from(order, payment);
    }

    /**
     * 8. 거래명세서 조회 - 주문 단건 데이터(OrderResponse)를 그대로 재사용한다.
     * API 명세서에도 "주문 단건 상세 조회 ... 하위 조회에서 공통으로 참조"라고 명시되어 있어
     * 별도 DTO를 새로 만들지 않고 getMyOrder()와 동일한 데이터를 반환한다.
     */
    public OrderResponse getStatement(Long userId, Long orderId) {
        return getMyOrder(userId, orderId);
    }

    /**
     * 상품 총액 기준 배송비를 계산한다.
     *
     * 이 값은 Order에 별도 컬럼으로 저장하지 않는다. finalPaymentAmount가
     * (totalProductAmount + 배송비 - 쿠폰할인 - 사용포인트)로 확정되므로,
     * 저장된 네 값에서 언제든 정확히 역산할 수 있기 때문이다.
     *
     * 따라서 이 메서드는 "새 주문의 배송비를 정할 때"만 쓴다. 이미 만들어진
     * 주문의 배송비를 알아낼 때는 정책 상수에 의존하지 않는 Order.getShippingFee()를 쓸 것.
     */
    private static int calcShippingFee(int totalProductAmount) {
        if (totalProductAmount <= 0) {
            return 0;
        }
        return totalProductAmount >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD" + timestamp + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
