package com.shoppingmall.domain.coupon.repository;

import com.shoppingmall.domain.coupon.entity.CouponRequest;
import com.shoppingmall.domain.coupon.entity.CouponRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 판매자 쿠폰 발행 요청 Repository
 */
public interface CouponRequestRepository
        extends JpaRepository<CouponRequest, Long> {

    /**
     * 동일 판매자가 동일한 쿠폰명으로
     * 승인 대기 요청을 가지고 있는지 확인한다.
     */
    boolean existsBySeller_IdAndCouponNameAndStatus(
            Long sellerId,
            String couponName,
            CouponRequestStatus status
    );

    /**
     * 판매자 본인의 쿠폰 발행 요청 목록 조회
     */
    Page<CouponRequest>
    findAllBySeller_IdOrderByCreatedAtDesc(
            Long sellerId,
            Pageable pageable
    );

    /**
     * 관리자용 상태별 쿠폰 요청 목록 조회
     */
    Page<CouponRequest>
    findAllByStatusOrderByCreatedAtAsc(
            CouponRequestStatus status,
            Pageable pageable
    );

    /**
     * 요청 ID와 상태를 동시에 검사한다.
     */
    Optional<CouponRequest> findByIdAndStatus(
            Long requestId,
            CouponRequestStatus status
    );
}