package com.shoppingmall.domain.notification.repository;

import com.shoppingmall.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** GET /api/v1/notifications - 내 알림 목록 (최신순) */
    Page<Notification> findAllByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** PATCH /api/v1/notifications/{notificationId}/read - 본인 알림인지 확인하며 단건 조회 */
    Optional<Notification> findByIdAndUser_Id(Long notificationId, Long userId);
}
