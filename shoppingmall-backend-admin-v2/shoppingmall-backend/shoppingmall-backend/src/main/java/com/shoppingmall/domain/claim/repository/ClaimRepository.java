package com.shoppingmall.domain.claim.repository;

import com.shoppingmall.domain.claim.entity.Claim;
import com.shoppingmall.domain.claim.entity.ClaimStatus;
import com.shoppingmall.domain.claim.entity.ClaimType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

/**
 * 교환·환불 클레임 Repository
 */
public interface ClaimRepository
        extends JpaRepository<Claim, Long> {

    /**
     * 판매자에게 접수된 전체 클레임 조회
     */
    Page<Claim>
    findAllByOrderDetail_Product_Seller_IdOrderByCreatedAtDesc(
            Long sellerId,
            Pageable pageable
    );

    /**
     * 판매자의 클레임 유형별 조회
     */
    Page<Claim>
    findAllByOrderDetail_Product_Seller_IdAndTypeOrderByCreatedAtDesc(
            Long sellerId,
            ClaimType type,
            Pageable pageable
    );

    /**
     * 판매자의 클레임 상태별 조회
     */
    Page<Claim>
    findAllByOrderDetail_Product_Seller_IdAndStatusOrderByCreatedAtDesc(
            Long sellerId,
            ClaimStatus status,
            Pageable pageable
    );

    /**
     * 판매자의 유형 및 상태별 클레임 조회
     */
    Page<Claim>
    findAllByOrderDetail_Product_Seller_IdAndTypeAndStatusOrderByCreatedAtDesc(
            Long sellerId,
            ClaimType type,
            ClaimStatus status,
            Pageable pageable
    );

    /**
     * 사용자가 신청한 클레임 목록 조회
     */
    Page<Claim>
    findAllByOrderDetail_Order_User_IdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    /**
     * 사용자 소유의 클레임 단건 조회
     */
    Optional<Claim> findByIdAndOrderDetail_Order_User_Id(
            Long claimId,
            Long userId
    );

    /**
     * 동일 주문 상세에 처리 중인 클레임이 있는지 확인
     */
    boolean existsByOrderDetail_IdAndStatusIn(
            Long orderDetailId,
            Collection<ClaimStatus> statuses
    );

    /**
     * 특정 판매자 상품에 접수된 특정 상태의 클레임 수 조회
     *
     * 판매자 대시보드의 처리 대기 클레임 수 계산에 사용한다.
     */
    long countByOrderDetail_Product_Seller_IdAndStatus(
            Long sellerApplicationId,
            ClaimStatus status
    );
}