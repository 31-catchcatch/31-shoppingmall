package com.shoppingmall.domain.payment.service;

import com.shoppingmall.domain.payment.dto.request.PaymentMethodCreateRequest;
import com.shoppingmall.domain.payment.dto.response.PaymentMethodResponse;
import com.shoppingmall.domain.payment.entity.PaymentMethod;
import com.shoppingmall.domain.payment.repository.PaymentMethodRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** API 명세서 "일반 사용자 - 마이페이지 - 결제수단" 대응 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;

    /** GET /api/v1/users/me/payments */
    public List<PaymentMethodResponse> getMyPaymentMethods(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return paymentMethodRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .map(PaymentMethodResponse::from)
                .toList();
    }

    /** POST /api/v1/users/me/payments - 새 결제수단(빌링키) 등록 */
    @Transactional
    public void registerPaymentMethod(Long userId, PaymentMethodCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 새로 등록하는 결제수단을 기본으로 지정하면, 기존 기본 결제수단은 해제한다
        if (request.isDefaultMethod()) {
            paymentMethodRepository.findByUserAndDefaultMethodTrue(user)
                    .ifPresent(existing -> existing.updateDefaultStatus(false));
        }

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(user)
                .pgProvider(request.getPgProvider())
                .billingKey(request.getBillingKey())
                .alias(request.getAlias())
                .maskedCardNumber(request.getMaskedCardNumber())
                .defaultMethod(request.isDefaultMethod())
                .build();

        paymentMethodRepository.save(paymentMethod);
    }

    /** DELETE /api/v1/users/me/payments/{paymentId} - 본인 소유 결제수단만 삭제 허용 */
    @Transactional
    public void deletePaymentMethod(Long userId, Long paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUser_Id(paymentMethodId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));
        paymentMethodRepository.delete(paymentMethod);
    }
}
