package com.shoppingmall.domain.admin.dto.request;

/** PATCH /api/v1/admin/users/{userId}/status - 계정 정지/정지해제 */
public record UserStatusUpdateRequest(
        boolean suspended
) {
}
