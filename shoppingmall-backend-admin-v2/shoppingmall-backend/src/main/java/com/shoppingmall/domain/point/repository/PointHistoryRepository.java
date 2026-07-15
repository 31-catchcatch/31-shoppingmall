package com.shoppingmall.domain.point.repository;

import com.shoppingmall.domain.point.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
