package com.shoppingmall.domain.point.dto.response;

import com.shoppingmall.domain.point.entity.PointHistory;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long historyId,
        int amount,
        int balanceAfter,
        String reason,
        LocalDateTime createdAt
) {
    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
                history.getId(),
                history.getAmount(),
                history.getBalanceAfter(),
                history.getReason(),
                history.getCreatedAt()
        );
    }
}
