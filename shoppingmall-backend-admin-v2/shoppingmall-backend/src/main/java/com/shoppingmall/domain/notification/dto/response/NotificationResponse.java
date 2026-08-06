package com.shoppingmall.domain.notification.dto.response;

import com.shoppingmall.domain.notification.entity.Notification;
import com.shoppingmall.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

/** GET /api/v1/notifications 목록의 개별 항목 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String content,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
