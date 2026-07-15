package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerDeliveryUpdateRequest;
import com.shoppingmall.domain.seller.dto.request.SellerOrderSearchRequest;
import com.shoppingmall.domain.seller.dto.response.SellerDeliveryResponse;
import com.shoppingmall.domain.seller.dto.response.SellerOrderListResponse;
import com.shoppingmall.domain.seller.service.SellerOrderService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 주문 및 배송 관리 API
 *
 * GET /api/v1/seller/orders
 * PUT /api/v1/seller/orders/{orderDetailId}/delivery
 */
@RestController
@RequestMapping("/api/v1/seller/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;

    /**
     * 판매자에게 접수된 주문 상품 목록 조회
     *
     * 요청 예시:
     * GET /api/v1/seller/orders
     * GET /api/v1/seller/orders?deliveryStatus=PREPARING&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SellerOrderListResponse>> getOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute SellerOrderSearchRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerOrderListResponse response = sellerOrderService.getOrders(userId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{orderDetailId}/delivery")
    public ResponseEntity<ApiResponse<SellerDeliveryResponse>> updateDelivery(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long orderDetailId,
            @Valid @RequestBody SellerDeliveryUpdateRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerDeliveryResponse response =
                sellerOrderService.updateDelivery(userId, orderDetailId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
