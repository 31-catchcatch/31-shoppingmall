package com.shoppingmall.domain.order.service;

import com.shoppingmall.domain.cart.repository.CartItemRepository;
import com.shoppingmall.domain.order.dto.request.OrderCreateRequest;
import com.shoppingmall.domain.order.dto.response.CheckoutResponse;
import com.shoppingmall.domain.order.dto.response.OrderListResponse;
import com.shoppingmall.domain.order.dto.response.OrderResponse;
import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.entity.Order;
import com.shoppingmall.domain.order.entity.OrderDetail;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.repository.OrderDetailRepository;
import com.shoppingmall.domain.order.repository.OrderRepository;
import com.shoppingmall.domain.product.entity.Product;
import com.shoppingmall.domain.product.entity.ProductOption;
import com.shoppingmall.domain.product.repository.ProductRepository;
import com.shoppingmall.domain.user.entity.Address;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.AddressRepository;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

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
                .build();
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

        List<OrderDetail> details = new ArrayList<>();
        int totalProductAmount = 0;

        for (OrderCreateRequest.OrderItemRequest item : request.items()) {
            Product product = productRepository.findByIdAndDeletedFalse(item.productId())
                    .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

            ProductOption option = null;
            int optionAdditionalPrice = 0;
            if (item.optionId() != null) {
                option = product.getOptions().stream()
                        .filter(o -> o.getId().equals(item.optionId()))
                        .findFirst()
                        .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));

                if (option.getStockQuantity() < item.quantity()) {
                    throw new CustomException(ErrorCode.INVALID_INPUT);
                }
                optionAdditionalPrice = option.getAdditionalPrice();
            }

            int unitPrice = product.getPrice() + optionAdditionalPrice;

            details.add(OrderDetail.builder()
                    .product(product)
                    .productOption(option)
                    .unitPrice(unitPrice)
                    .quantity(item.quantity())
                    .build());

            totalProductAmount += unitPrice * item.quantity();

            // 장바구니에서 담아뒀던 항목이면 주문 완료 후 정리 (장바구니에 없던 즉시구매는 그냥 무시됨)
            Long optionId = option != null ? option.getId() : null;
            cartItemRepository.findByUserAndProductIdAndProductOptionId(user, product.getId(), optionId)
                    .ifPresent(cartItemRepository::delete);
        }

        int couponDiscountAmount = 0; // TODO: coupon 도메인 연동 후 실제 할인액 반영
        int finalPaymentAmount = totalProductAmount - couponDiscountAmount - usePoint;
        if (finalPaymentAmount < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .totalProductAmount(totalProductAmount)
                .couponDiscountAmount(couponDiscountAmount)
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
        return OrderResponse.from(saved);
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
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD" + timestamp + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
