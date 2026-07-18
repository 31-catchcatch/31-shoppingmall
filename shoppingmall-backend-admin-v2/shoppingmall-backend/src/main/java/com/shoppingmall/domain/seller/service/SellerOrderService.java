package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.order.entity.OrderDetail;
import com.shoppingmall.domain.order.entity.DeliveryStatus;
import com.shoppingmall.domain.order.repository.OrderDetailRepository;
import com.shoppingmall.domain.seller.dto.request.SellerDeliveryUpdateRequest;
import com.shoppingmall.domain.seller.dto.request.SellerOrderSearchRequest;
import com.shoppingmall.domain.seller.dto.response.SellerDeliveryResponse;
import com.shoppingmall.domain.seller.dto.response.SellerOrderListResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 주문 및 배송 관리 서비스
 *
 * 담당 API
 * GET /api/v1/seller/orders
 * PUT /api/v1/seller/orders/{orderDetailId}/delivery
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerOrderService {

    private final OrderDetailRepository orderDetailRepository;
    private final SellerApplicationRepository sellerApplicationRepository;

    /**
     * 판매자에게 접수된 주문 상품 목록 조회.
     * 배송 상태가 전달되면 해당 상태만 조회하고, 없으면 전체 조회한다.
     */
    public SellerOrderListResponse getOrders(Long userId, SellerOrderSearchRequest request) {
        SellerApplication sellerApplication = getApprovedSellerApplication(userId);

        int page = request.page() == null ? 0 : Math.max(request.page(), 0);
        int size = request.size() == null ? 20 : Math.min(Math.max(request.size(), 1), 100);
        Pageable pageable = PageRequest.of(page, size);

        Page<OrderDetail> orderPage;

        if (request.deliveryStatus() == null || request.deliveryStatus().isBlank()) {
            orderPage = orderDetailRepository
                    .findAllByProduct_Seller_IdOrderByCreatedAtDesc(sellerApplication.getId(), pageable);
        } else {
            DeliveryStatus deliveryStatus;
            try {
                deliveryStatus = DeliveryStatus.valueOf(request.deliveryStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
            }

            orderPage = orderDetailRepository.findAllByProduct_Seller_IdAndDeliveryStatusOrderByCreatedAtDesc(
                    sellerApplication.getId(), deliveryStatus, pageable);
        }

        return SellerOrderListResponse.from(orderPage);
    }

    /**
     * 주문 상품의 배송 정보와 상태를 변경한다.
     *
     * 처리 순서
     * 1. 로그인 사용자가 승인된 판매자인지 확인
     * 2. 주문 상세 조회
     * 3. 해당 판매자의 상품 주문인지 확인
     * 4. 현재 출고 가능한 주문 상태인지 확인
     * 5. 택배사와 운송장 번호 등록
     * 6. 배송 상태 변경
     * 7. 응답 DTO 반환
     *
     * @param userId        로그인한 사용자 ID
     * @param orderDetailId 주문 상세 ID
     * @param request       택배사 및 운송장 번호
     * @return 변경된 배송 정보
     */
    @Transactional
    public SellerDeliveryResponse updateDelivery(
            Long userId,
            Long orderDetailId,
            SellerDeliveryUpdateRequest request
    ) {
        // 1. 승인된 판매자 신청 정보를 조회한다.
        SellerApplication sellerApplication =
                getApprovedSellerApplication(userId);

        // 2. 주문 상세 정보를 조회한다.
        OrderDetail orderDetail = orderDetailRepository
                .findById(orderDetailId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.ORDER_NOT_FOUND)
                );

        // 3. 현재 판매자가 주문받은 상품인지 확인한다.
        validateOrderOwnership(
                orderDetail,
                sellerApplication
        );

        // 4. 현재 상태에서 배송 처리가 가능한지 확인한다.
        validateDeliveryStatus(orderDetail);

        // 5. 택배사, 운송장 번호, 배송 상태를 변경한다.
        orderDetail.registerDelivery(
                request.courierCompany(),
                request.trackingNumber()
        );

        // 6. 변경된 주문 배송 정보를 응답 DTO로 변환한다.
        return SellerDeliveryResponse.from(orderDetail);
    }

    /**
     * 배송 중인 주문을 배송완료로 처리한다.
     * (이 처리 없이는 구매자가 구매확정을 할 수 없고, 정산도 생성되지 않는다.)
     */
    @Transactional
    public SellerDeliveryResponse completeDelivery(
            Long userId,
            Long orderDetailId
    ) {
        SellerApplication sellerApplication =
                getApprovedSellerApplication(userId);

        OrderDetail orderDetail = orderDetailRepository
                .findById(orderDetailId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.ORDER_NOT_FOUND)
                );

        validateOrderOwnership(
                orderDetail,
                sellerApplication
        );

        if (orderDetail.getDeliveryStatus() != DeliveryStatus.SHIPPING) {
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }

        orderDetail.completeDelivery();

        return SellerDeliveryResponse.from(orderDetail);
    }

    /**
     * 가장 최근 입점 신청이 승인 상태인지 확인한다.
     */
    private SellerApplication getApprovedSellerApplication(
            Long userId
    ) {
        SellerApplication application =
                sellerApplicationRepository
                        .findFirstByUser_IdOrderByCreatedAtDesc(userId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SELLER_NOT_APPROVED
                                )
                        );

        if (application.getStatus()
                != SellerApplicationStatus.APPROVED) {
            throw new CustomException(
                    ErrorCode.SELLER_NOT_APPROVED
            );
        }

        return application;
    }

    /**
     * 주문 상품이 현재 로그인한 판매자의 상품인지 확인한다.
     *
     * OrderDetail → Product → SellerApplication 관계를 이용한다.
     */
    private void validateOrderOwnership(
            OrderDetail orderDetail,
            SellerApplication sellerApplication
    ) {
        Long productSellerId = orderDetail
                .getProduct()
                .getSeller()
                .getId();

        if (!productSellerId.equals(sellerApplication.getId())) {
            throw new CustomException(
                    ErrorCode.ACCESS_DENIED
            );
        }
    }

    /**
     * 결제 완료 또는 상품 준비 중 상태의 주문만
     * 판매자가 배송 처리할 수 있도록 검증한다.
     */
    private void validateDeliveryStatus(
            OrderDetail orderDetail
    ) {
        DeliveryStatus status =
                orderDetail.getDeliveryStatus();

        if (status != DeliveryStatus.PAYMENT_COMPLETED
                && status != DeliveryStatus.PREPARING) {
            throw new CustomException(
                    ErrorCode.INVALID_ORDER_STATUS
            );
        }
    }
}