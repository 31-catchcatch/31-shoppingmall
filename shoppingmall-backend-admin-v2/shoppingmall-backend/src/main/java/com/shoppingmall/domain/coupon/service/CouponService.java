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
     * 마이페이지 '받기' 탭 - 로그인 사용자가 아직 받지 않은 발급 가능 쿠폰 목록 조회.
     * 이미 발급받은 쿠폰은 제외하여, 목록에 뜨는 쿠폰은 모두 '받기' 가능하다.
     */
    public CouponListResponse getAvailableCoupons(
            Long userId,
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
                .findClaimableByUser(userId, now, pageable);

        return CouponListResponse.from(coupons);
    }
}