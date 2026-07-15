package com.shoppingmall.domain.notification.dto.request;

import com.shoppingmall.domain.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationSendRequest(
        @NotNull Long userId,
        @NotNull NotificationType type,
        @NotBlank String title,
        @NotBlank String content
) {
}
