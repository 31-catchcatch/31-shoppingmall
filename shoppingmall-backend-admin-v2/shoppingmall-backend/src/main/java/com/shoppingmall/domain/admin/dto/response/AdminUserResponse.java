package com.shoppingmall.domain.admin.dto.response;

import com.shoppingmall.domain.user.entity.User;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long userId,
        String username,
        String name,
        String email,
        String role,
        int point,
        boolean deleted,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getPoint(),
                user.isDeleted(),
                user.getCreatedAt()
        );
    }
}
