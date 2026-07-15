package com.shoppingmall.domain.seller.repository;

import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SellerApplicationRepository
        extends JpaRepository<SellerApplication, Long> {

    // 특정 사용자가 심사 중이거나 승인된 신청서를 가지고 있는지 확인
    boolean existsByUser_IdAndStatusIn(
            Long userId,
            Collection<SellerApplicationStatus> statuses
    );

    // 특정 사업자등록번호로 심사 중이거나 승인된 신청서가 있는지 확인
    boolean existsByBusinessRegistrationNumberAndStatusIn(
            String businessRegistrationNumber,
            Collection<SellerApplicationStatus> statuses
    );

    // 사용자의 입점 신청 내역 조회
    List<SellerApplication> findAllByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    // 사용자의 가장 최근 입점 신청 조회
    Optional<SellerApplication> findFirstByUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    // 관리자가 특정 상태의 신청서 목록을 조회할 때 사용
    List<SellerApplication> findAllByStatusOrderByCreatedAtAsc(
            SellerApplicationStatus status
    );
}