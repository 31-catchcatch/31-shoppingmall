package com.shoppingmall.domain.notification.dto.request;

import com.shoppingmall.global.validation.NoHtml;
import jakarta.validation.constraints.Size;

import com.shoppingmall.domain.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationSendRequest(
        @NotNull Long userId,
        @NotNull NotificationType type,
        @NotBlank @Size(max = 200) @NoHtml String title,       // [1-1][1-6]
        @NotBlank @Size(max = 2000) @NoHtml String content     // [1-1][1-6]
) {
}
