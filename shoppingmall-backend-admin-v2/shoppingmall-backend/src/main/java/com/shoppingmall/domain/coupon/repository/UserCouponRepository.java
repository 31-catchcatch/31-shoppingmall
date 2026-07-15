package com.shoppingmall.domain.coupon.repository;

import com.shoppingmall.domain.coupon.entity.UserCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    /**
     * GET /api/v1/users/me/coupons - 본인이 보유한 "사용 가능한" 쿠폰만 조회.
     * 사용하지 않았고(coupon.isUsed = false), 쿠폰 자체가 활성 상태이며 유효기간 이내인 것만 반환한다.
     */
    @Query("""
            select uc from UserCoupon uc
            join fetch uc.coupon c
            where uc.user.id = :userId
              and uc.used = false
              and c.active = true
              and c.validFrom <= :now
              and c.validUntil >= :now
            order by uc.createdAt desc
            """)
    Page<UserCoupon> findMyAvailableCoupons(@Param("userId") Long userId,
                                             @Param("now") LocalDateTime now,
                                             Pageable pageable);
}
