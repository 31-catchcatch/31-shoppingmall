package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.request.AdminCouponCreateRequest;
import com.shoppingmall.domain.admin.dto.request.ReviewDecisionRequest;
import com.shoppingmall.domain.admin.dto.response.AdminCouponResponse;
import com.shoppingmall.domain.coupon.dto.response.CouponRequestResponse;
import com.shoppingmall.domain.coupon.entity.Coupon;
import com.shoppingmall.domain.coupon.entity.CouponRequest;
import com.shoppingmall.domain.coupon.entity.CouponRequestStatus;
import com.shoppingmall.domain.coupon.repository.CouponRepository;
import com.shoppingmall.domain.coupon.repository.CouponRequestRepository;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * API 명세서 "관리자 - 운영 - 쿠폰" 담당.
 * - GET  /admin/coupons
 * - POST /admin/coupons
 * - GET  /admin/coupons/requests
 * - PUT  /admin/coupons/requests/{requestId}
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCouponService {

    private final CouponRequestRepository couponRequestRepository;
    private final CouponRepository couponRepository;
    private final SellerApplicationRepository sellerApplicationRepository;

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

    /** 발행된 쿠폰 전체 목록. 판매자 요청을 승인한 것과 관리자가 직접 발행한 것이 함께 나온다. */
    public PageResponse<AdminCouponResponse> getIssuedCoupons(Pageable pageable) {
        Page<AdminCouponResponse> page = couponRepository.findAllForAdmin(pageable)
                .map(AdminCouponResponse::from);
        return PageResponse.from(page);
    }

    /**
     * 관리자가 판매자 요청 없이 쿠폰을 바로 발행한다.
     *
     * <p>쿠폰(coupons)은 {@code seller_id} 와 {@code coupon_request_id} 가 둘 다 NOT NULL 이다.
     * 그래서 직접 발행이라도 <b>승인 상태의 발행 요청을 함께 만들어</b> 두 컬럼을 채운다.
     * 스키마를 손대지 않고 두 경로가 같은 테이블을 쓰게 하는 방식이다.
     *
     * <p>검증 규칙은 판매자 경로(SellerCouponService)와 일부러 동일하게 맞췄다.
     * 어긋나면 "판매자는 못 넣는 값을 관리자는 넣을 수 있는" 구멍이 생긴다.
     *
     * <ol>
     *   <li>대상 판매자가 승인된 입점업체인지 확인
     *   <li>유효기간 검증
     *   <li>할인 유형·값 검증
     *   <li>같은 이름으로 심사 중인 요청이 있는지 확인
     *   <li>APPROVED 상태 CouponRequest 생성 → 저장
     *   <li>그 요청으로 실제 Coupon 발행 → 저장
     * </ol>
     */
    @Transactional
    public AdminCouponResponse createCoupon(AdminCouponCreateRequest request) {
        // 1. 쿠폰은 그 판매자 상품에만 적용되므로 대상 업체가 유효해야 한다.
        //    ⚠️ 여기서 찾는 것은 Seller 가 아니라 SellerApplication 이다.
        //       Coupon.seller / CouponRequest.seller 가 둘 다 SellerApplication 을 가리킨다.
        SellerApplication seller = sellerApplicationRepository.findById(request.sellerId())
                .orElseThrow(() -> new CustomException(ErrorCode.SELLER_NOT_APPROVED));

        if (seller.getStatus() != SellerApplicationStatus.APPROVED) {
            throw new CustomException(ErrorCode.SELLER_NOT_APPROVED);
        }

        // 2~3. 기간과 할인 값을 검증한다.
        validateCouponPeriod(request);
        validateDiscount(request);

        // 4. 같은 이름으로 심사 대기 중인 요청이 있으면 막는다.
        //    그대로 두면 관리자가 직접 발행한 뒤 그 요청까지 승인돼 같은 쿠폰이 두 장 생긴다.
        boolean alreadyExists = couponRequestRepository.existsBySeller_IdAndCouponNameAndStatus(
                seller.getId(),
                request.couponName(),
                CouponRequestStatus.PENDING
        );

        if (alreadyExists) {
            throw new CustomException(ErrorCode.COUPON_REQUEST_ALREADY_EXISTS);
        }

        // 5. 발행 근거가 되는 요청을 승인 상태로 만든다.
        //    approve() 를 한 번 더 부르는 이유는 reviewedAt 을 남기기 위해서다.
        CouponRequest couponRequest = CouponRequest.builder()
                .seller(seller)
                .couponName(request.couponName())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minimumOrderAmount(request.minimumOrderAmount())
                .maximumDiscountAmount(request.maximumDiscountAmount())
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .totalQuantity(request.totalQuantity())
                .status(CouponRequestStatus.APPROVED)
                .build();
        couponRequest.approve();

        CouponRequest savedRequest = couponRequestRepository.save(couponRequest);

        // 6. 승인 흐름과 똑같이 Coupon.from 으로 실제 쿠폰을 만든다.
        Coupon coupon = couponRepository.save(Coupon.from(savedRequest));

        return AdminCouponResponse.from(coupon);
    }

    /** 쿠폰 종료일이 시작일보다 이후인지 확인한다. */
    private void validateCouponPeriod(AdminCouponCreateRequest request) {
        if (!request.validUntil().isAfter(request.validFrom())) {
            throw new CustomException(ErrorCode.INVALID_COUPON_PERIOD);
        }
    }

    /**
     * 할인 유형과 할인 값을 검증한다.
     *
     * PERCENTAGE: 할인율 1~100
     * FIXED_AMOUNT: 할인 금액 0 초과
     */
    private void validateDiscount(AdminCouponCreateRequest request) {
        String discountType = request.discountType().trim().toUpperCase();

        if ("PERCENTAGE".equals(discountType)) {
            if (request.discountValue().compareTo(BigDecimal.ONE) < 0
                    || request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new CustomException(ErrorCode.INVALID_COUPON_DISCOUNT);
            }

            return;
        }

        if ("FIXED_AMOUNT".equals(discountType)) {
            if (request.discountValue().signum() <= 0) {
                throw new CustomException(ErrorCode.INVALID_COUPON_DISCOUNT);
            }

            return;
        }

        throw new CustomException(ErrorCode.INVALID_COUPON_DISCOUNT);
    }
}
