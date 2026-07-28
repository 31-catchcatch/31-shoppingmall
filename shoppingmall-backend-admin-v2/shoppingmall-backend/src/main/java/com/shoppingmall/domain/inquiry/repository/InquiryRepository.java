package com.shoppingmall.domain.inquiry.repository;

import com.shoppingmall.domain.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /** 내 문의 내역 조회 (최신순) */
    Page<Inquiry> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** 본인 소유 문의 단건 조회 (소유권 + 존재 동시 판정) */
    Optional<Inquiry> findByIdAndUser_Id(Long id, Long userId);
}
