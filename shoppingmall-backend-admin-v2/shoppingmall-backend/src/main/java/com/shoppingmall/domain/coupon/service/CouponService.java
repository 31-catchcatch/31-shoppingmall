package com.shoppingmall.domain.coupon.service;

import com.shoppingmall.domain.coupon.dto.response.CouponListResponse;
import com.shoppingmall.domain.coupon.dto.response.CouponResponse;
import com.shoppingmall.domain.coupon.entity.Coupon;
import com.shoppingmall.domain.coupon.entity.CouponRequest;
import com.shoppingmall.domain.coupon.entity.CouponRequestStatus;
import com.shoppingmall.domain.coupon.repository.CouponRepository;
import com.shoppingmall.domain.coupon.repository.CouponRequestRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 실제 쿠폰 생성 및 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRequestRepository couponRequestRepository;

    /**
     * 관리자가 판매자의 쿠폰 요청을 승인하고
     * 실제 사용할 Coupon Entity를 생성한다.
     */
    @Transactional
    public CouponResponse approveAndCreateCoupon(
            Long requestId
    ) {
        CouponRequest request = couponRequestRepository
                .findByIdAndStatus(
                        requestId,
                        CouponRequestStatus.PENDING
                )
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.COUPON_REQUEST_NOT_FOUND
                        )
                );

        if (couponRepository
                .existsByCouponRequest_Id(requestId)) {
            throw new CustomException(
                    ErrorCode.COUPON_REQUEST_ALREADY_EXISTS
            );
        }

        request.approve();

        Coupon coupon = Coupon.from(request);

        Coupon savedCoupon =
                couponRepository.save(coupon);

        return CouponResponse.from(savedCoupon);
    }

    /**
     * 현재 사용 가능한 전체 쿠폰 목록 조회
     */
    public CouponListResponse getAvailableCoupons(
            int page,
            int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize =
                Math.min(Math.max(size, 1), 100);

        PageRequest pageable = PageRequest.of(
                normalizedPage,
                normalizedSize
        );

        LocalDateTime now = LocalDateTime.now();

        Page<Coupon> coupons = couponRepository
                .findAllByActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
                        now,
                        now,
                        pageable
                );

        return CouponListResponse.from(coupons);
    }
}