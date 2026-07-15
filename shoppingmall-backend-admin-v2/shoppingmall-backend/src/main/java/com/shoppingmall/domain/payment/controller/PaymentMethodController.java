package com.shoppingmall.domain.payment.controller;

import com.shoppingmall.domain.payment.dto.request.PaymentMethodCreateRequest;
import com.shoppingmall.domain.payment.dto.response.PaymentMethodResponse;
import com.shoppingmall.domain.payment.service.PaymentMethodService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API 명세서 "일반 사용자 - 마이페이지 - 결제수단" 매핑 (로그인 필요) */
@RestController
@RequestMapping("/api/v1/users/me/payments")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> getMyPaymentMethods(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<PaymentMethodResponse> response = paymentMethodService.getMyPaymentMethods(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> registerPaymentMethod(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentMethodCreateRequest request) {
        paymentMethodService.registerPaymentMethod(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("결제 수단이 등록되었습니다.", null));
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<Void>> deletePaymentMethod(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long paymentId) {
        paymentMethodService.deletePaymentMethod(userDetails.getUser().getId(), paymentId);
        return ResponseEntity.ok(ApiResponse.success("결제 수단이 삭제되었습니다.", null));
    }
}
