package com.shoppingmall.domain.coupon.repository;

import com.shoppingmall.domain.coupon.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * GET /api/v1/coupons - "내가 아직 받지 않은" 발급 가능 쿠폰 조회 (마이페이지 '받기' 탭).
     * 활성 + 유효기간 내이면서, 해당 사용자가 이미 발급받지(UserCoupon) 않은 쿠폰만 반환한다.
     */
    @Query("""
            select c from Coupon c
            where c.active = true
              and c.validFrom <= :now
              and c.validUntil >= :now
              and not exists (
                    select 1 from UserCoupon uc
                    where uc.coupon = c and uc.user.id = :userId
              )
            order by c.createdAt desc
            """)
    Page<Coupon> findClaimableByUser(@Param("userId") Long userId,
                                     @Param("now") LocalDateTime now,
                                     Pageable pageable);

    /**
     * GET /api/v1/admin/coupons - 관리자 콘솔 '발행 쿠폰' 탭.
     * 중지(active=false)된 쿠폰도 화면에서 상태로 구분해 보여주므로 전부 내려준다.
     *
     * 응답(AdminCouponResponse)에 판매자 상호명과 요청 ID가 들어간다.
     * 지연 로딩으로 두면 화면이 한 번에 200건을 부르는 만큼 판매자 조회가 따라붙으므로
     * 두 연관을 함께 읽는다. 둘 다 ToOne 이라 페이징이 메모리로 내려가지 않는다.
     */
    @Query(
            value = """
                    select c from Coupon c
                    join fetch c.seller
                    join fetch c.couponRequest
                    order by c.createdAt desc
                    """,
            countQuery = "select count(c) from Coupon c"
    )
    Page<Coupon> findAllForAdmin(Pageable pageable);

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