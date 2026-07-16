package com.shoppingmall.domain.coupon.service;

import com.shoppingmall.domain.coupon.dto.response.UserCouponResponse;
import com.shoppingmall.domain.coupon.entity.Coupon;
import com.shoppingmall.domain.coupon.entity.UserCoupon;
import com.shoppingmall.domain.coupon.repository.CouponRepository;
import com.shoppingmall.domain.coupon.repository.UserCouponRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * API 명세서 "일반 사용자 - 마이페이지 - 보유 쿠폰 목록 조회" 대응.
 * 판매자/관리자 쿠폰 발행-승인 흐름(CouponService)과는 다른 관심사라 별도 서비스로 분리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCouponService {

    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    public Page<UserCouponResponse> getMyAvailableCoupons(Long userId, Pageable pageable) {
        return userCouponRepository.findMyAvailableCoupons(userId, LocalDateTime.now(), pageable)
                .map(UserCouponResponse::from);
    }

    /**
     * POST /api/v1/coupons/{couponId}/claim - 쿠폰을 내 지갑(user_coupons)으로 발급받는다.
     * user_coupons에 데이터가 쌓이는 유일한 경로이며, 이게 있어야 보유 쿠폰 조회/주문 쿠폰 적용이 실제로 동작한다.
     */
    @Transactional
    public void claimCoupon(Long userId, Long couponId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CustomException(ErrorCode.COUPON_NOT_AVAILABLE));

        if (!coupon.isUsable(LocalDateTime.now())) {
            // 비활성/기간외/수량소진 모두 포함 - 수량소진은 별도 코드로 구분
            if (coupon.getIssuedQuantity() >= coupon.getTotalQuantity()) {
                throw new CustomException(ErrorCode.COUPON_SOLD_OUT);
            }
            throw new CustomException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        if (userCouponRepository.existsByUser_IdAndCoupon_Id(userId, couponId)) {
            throw new CustomException(ErrorCode.COUPON_ALREADY_CLAIMED);
        }

        coupon.issue(); // 발급 수량 +1 (한도 초과 시 엔티티에서 예외)
        userCouponRepository.save(UserCoupon.builder().user(user).coupon(coupon).build());
    }
}
