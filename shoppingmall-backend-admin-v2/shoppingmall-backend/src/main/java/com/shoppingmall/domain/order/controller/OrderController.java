package com.shoppingmall.domain.order.controller;

import com.shoppingmall.domain.order.dto.request.OrderCreateRequest;
import com.shoppingmall.domain.order.dto.response.CheckoutResponse;
import com.shoppingmall.domain.order.dto.response.OrderDeliveryResponse;
import com.shoppingmall.domain.order.dto.response.OrderListResponse;
import com.shoppingmall.domain.order.dto.response.OrderReceiptResponse;
import com.shoppingmall.domain.order.dto.response.OrderResponse;
import com.shoppingmall.domain.order.entity.OrderStatus;
import com.shoppingmall.domain.order.service.OrderService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API 명세서 "일반 사용자 - 주문/결제" 도메인 매핑 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** GET /api/v1/orders/checkout - 주문서 페이지 조회 */
    @GetMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> getCheckout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CheckoutResponse response = orderService.getCheckoutData(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** POST /api/v1/orders - 주문 및 결제 진행 (최종 주문서 생성) */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.placeOrder(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("주문이 정상적으로 생성되었습니다.", response));
    }

    /** GET /api/v1/orders - 주문 내역 조회 (상태 필터 선택) */
    @GetMapping
    public ResponseEntity<ApiResponse<OrderListResponse>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        OrderListResponse response = orderService.getMyOrders(userDetails.getUser().getId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/v1/orders/{orderId} - 주문 단건 조회 (영수증/명세서 등에서 재사용 가능) */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        OrderResponse response = orderService.getMyOrder(userDetails.getUser().getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** PUT /api/v1/orders/{orderDetailId}/confirm - 구매 확정 처리 */
    @PutMapping("/{orderDetailId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPurchase(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderDetailId) {
        orderService.confirmPurchase(userDetails.getUser().getId(), orderDetailId);
        return ResponseEntity.ok(ApiResponse.success("구매 확정이 완료되었습니다.", null));
    }

    /** GET /api/v1/orders/{orderId}/delivery - 배송 상태 조회 */
    @GetMapping("/{orderId}/delivery")
    public ResponseEntity<ApiResponse<List<OrderDeliveryResponse>>> getDelivery(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        List<OrderDeliveryResponse> response = orderService.getDeliveryInfo(userDetails.getUser().getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/v1/orders/{orderId}/receipt - 영수증 조회 */
    @GetMapping("/{orderId}/receipt")
    public ResponseEntity<ApiResponse<OrderReceiptResponse>> getReceipt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        OrderReceiptResponse response = orderService.getReceipt(userDetails.getUser().getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/v1/orders/{orderId}/statement - 거래명세서 조회 */
    @GetMapping("/{orderId}/statement")
    public ResponseEntity<ApiResponse<OrderResponse>> getStatement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderId) {
        OrderResponse response = orderService.getStatement(userDetails.getUser().getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // TODO POST /api/v1/orders/{orderId}/claims - 교환/환불 신청 (claim 도메인의 ClaimController 참고)
}
