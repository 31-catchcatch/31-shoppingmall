package com.shoppingmall.domain.coupon.repository;

import com.shoppingmall.domain.coupon.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 실제 발행 쿠폰 Repository
 */
public interface CouponRepository
        extends JpaRepository<Coupon, Long> {

    /**
     * 특정 판매자가 발행한 쿠폰 목록
     */
    Page<Coupon> findAllBySeller_IdOrderByCreatedAtDesc(
            Long sellerId,
            Pageable pageable
    );

    /**
     * 활성 쿠폰 목록
     */
    Page<Coupon> findAllByActiveTrueOrderByCreatedAtDesc(
            Pageable pageable
    );

    /**
     * 현재 유효기간 내에 있는 활성 쿠폰 조회
     */
    Page<Coupon>
    findAllByActiveTrueAndValidFromLessThanEqualAndValidUntilGreaterThanEqual(
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            Pageable pageable
    );

    /**
     * 승인 요청으로 생성된 쿠폰 조회
     */
    Optional<Coupon> findByCouponRequest_Id(
            Long couponRequestId
    );

    boolean existsByCouponRequest_Id(
            Long couponRequestId
    );
}