package com.shoppingmall.domain.payment.controller;

import com.shoppingmall.domain.payment.dto.request.PaymentConfirmRequest;
import com.shoppingmall.domain.payment.dto.request.PaymentVerifyRequest;
import com.shoppingmall.domain.payment.dto.response.PaymentConfigResponse;
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

    /**
     * GET /api/v1/payments/config - 결제창 호출에 필요한 환경값 조회 (비로그인 허용)
     *
     * 프론트에 빌드 스텝이 없어 환경별 값을 코드에 넣어둘 수 없으므로 서버가 런타임에 내려준다.
     * 응답에는 공개값인 clientKey 와 리다이렉트 URL만 담기며, secretKey 는 절대 포함하지 않는다.
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<PaymentConfigResponse>> getPaymentConfig() {
        return ResponseEntity.ok(
                ApiResponse.success("결제 설정 조회가 완료되었습니다.", paymentService.getPaymentConfig()));
    }

    /**
     * POST /api/v1/payments/confirm - 토스 결제 승인 (로그인 필요)
     *
     * 토스 결제창이 successUrl 로 넘겨준 paymentKey/orderId/amount 를 받아
     * 서버가 토스에 직접 승인을 요청하고 결과를 확정한다.
     *
     * 아래 /verify 는 결제창 없이 클라이언트 신고를 그대로 믿던 mock 경로다.
     * 프론트가 /confirm 으로 완전히 넘어간 것을 확인한 뒤 함께 제거할 예정이다.
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PaymentConfirmRequest request) {

        PaymentResponse response = paymentService.confirmPayment(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("결제 승인이 완료되었습니다.", response));
    }

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