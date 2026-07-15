package com.shoppingmall.domain.cart.controller;

import com.shoppingmall.domain.cart.dto.request.CartAddItemRequest;
import com.shoppingmall.domain.cart.dto.request.CartItemUpdateRequest;
import com.shoppingmall.domain.cart.dto.response.CartItemResponse;
import com.shoppingmall.domain.cart.service.CartService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // 1. 내 장바구니 목록 조회 (GET)
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getMyCart(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 💡 .getId()를 .getUser().getId()로 수정
        List<CartItemResponse> response = cartService.getMyCart(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("장바구니 목록 조회가 완료되었습니다.", response));
    }

    // 2. 장바구니 신규 추가 (POST) - 성공 시 바디 Void 처리
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CartAddItemRequest request) {
        // 💡 .getId()를 .getUser().getId()로 수정
        cartService.addCartItem(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("장바구니에 상품이 정상 추가되었습니다.", null));
    }

    // 3. 수량 업데이트 (PUT) - 성공 시 바디 Void 처리
    @PutMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> updateCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemUpdateRequest request) {
        // 💡 .getId()를 .getUser().getId()로 수정
        cartService.updateCartItem(userDetails.getUser().getId(), cartItemId, request);
        return ResponseEntity.ok(ApiResponse.success("장바구니 수량이 수정되었습니다.", null));
    }

    // 4. 개별 삭제 (DELETE) - 성공 시 바디 Void 처리
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long cartItemId) {
        // 💡 .getId()를 .getUser().getId()로 수정
        cartService.deleteCartItem(userDetails.getUser().getId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success("장바구니에서 해당 물품이 비워졌습니다.", null));
    }
}