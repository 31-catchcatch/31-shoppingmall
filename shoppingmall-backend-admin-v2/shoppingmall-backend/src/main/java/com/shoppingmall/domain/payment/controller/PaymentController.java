package com.shoppingmall.domain.payment.controller;

import com.shoppingmall.domain.payment.dto.request.PaymentVerifyRequest;
import com.shoppingmall.domain.payment.dto.response.PaymentResponse;
import com.shoppingmall.domain.payment.service.PaymentService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // PG 결제 완료 콜백 및 위조 결제 검증 승인 API (POST)
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentVerifyRequest request) {

        PaymentResponse response = paymentService.verifyAndSavePayment(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("결제 내역 검증 및 승인이 성공적으로 완료되었습니다.", response));
    }
}