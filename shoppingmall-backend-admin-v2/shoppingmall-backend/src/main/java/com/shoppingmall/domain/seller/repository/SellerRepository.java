package com.shoppingmall.domain.seller.repository;

import com.shoppingmall.domain.seller.entity.Seller;
import com.shoppingmall.domain.seller.entity.SellerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerRepository
        extends JpaRepository<Seller, Long> {

    // 사용자 ID로 판매자 조회
    Optional<Seller> findByUser_Id(Long userId);

    // 해당 사용자가 이미 판매자로 등록되어 있는지 확인
    boolean existsByUser_Id(Long userId);

    // 사업자등록번호로 판매자 조회
    Optional<Seller> findByBusinessRegistrationNumber(
            String businessRegistrationNumber
    );

    // 사업자등록번호 중복 확인
    boolean existsByBusinessRegistrationNumber(
            String businessRegistrationNumber
    );

    // 판매자 상태별 목록 조회
    List<Seller> findAllByStatus(SellerStatus status);

    // 특정 사용자의 정상 판매자 계정 조회
    Optional<Seller> findByUser_IdAndStatus(
            Long userId,
            SellerStatus status
    );
}