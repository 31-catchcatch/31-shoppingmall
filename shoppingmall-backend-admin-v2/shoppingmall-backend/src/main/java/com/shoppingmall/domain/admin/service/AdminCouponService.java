package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.request.ReviewDecisionRequest;
import com.shoppingmall.domain.coupon.dto.response.CouponRequestResponse;
import com.shoppingmall.domain.coupon.entity.Coupon;
import com.shoppingmall.domain.coupon.entity.CouponRequest;
import com.shoppingmall.domain.coupon.entity.CouponRequestStatus;
import com.shoppingmall.domain.coupon.repository.CouponRepository;
import com.shoppingmall.domain.coupon.repository.CouponRequestRepository;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 명세서 "관리자 - 운영 - 쿠폰" 담당.
 * - GET /admin/coupons/requests
 * - PUT /admin/coupons/requests/{requestId}
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCouponService {

    private final CouponRequestRepository couponRequestRepository;
    private final CouponRepository couponRepository;

    public PageResponse<CouponRequestResponse> getPendingRequests(Pageable pageable) {
        Page<CouponRequestResponse> page = couponRequestRepository
                .findAllByStatusOrderByCreatedAtAsc(CouponRequestStatus.PENDING, pageable)
                .map(CouponRequestResponse::from);
        return PageResponse.from(page);
    }

    /** 승인 시 CouponRequest.approve() + 실제 Coupon 엔티티 발행(Coupon.from) 까지 처리 */
    @Transactional
    public CouponRequestResponse reviewRequest(Long requestId, ReviewDecisionRequest request) {
        CouponRequest couponRequest = couponRequestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_REQUEST_NOT_FOUND));

        if (couponRequest.getStatus() != CouponRequestStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_SELLER_STATUS);
        }

        if (request.decision() == ReviewDecisionRequest.Decision.APPROVE) {
            couponRequest.approve();
            couponRepository.save(Coupon.from(couponRequest));
        } else {
            couponRequest.reject(request.rejectionReason());
        }

        return CouponRequestResponse.from(couponRequest);
    }
}
