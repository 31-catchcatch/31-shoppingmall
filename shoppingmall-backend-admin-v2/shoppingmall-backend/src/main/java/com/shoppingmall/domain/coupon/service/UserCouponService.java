package com.shoppingmall.domain.coupon.service;

import com.shoppingmall.domain.coupon.dto.response.UserCouponResponse;
import com.shoppingmall.domain.coupon.repository.UserCouponRepository;
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

    public Page<UserCouponResponse> getMyAvailableCoupons(Long userId, Pageable pageable) {
        return userCouponRepository.findMyAvailableCoupons(userId, LocalDateTime.now(), pageable)
                .map(UserCouponResponse::from);
    }
}
