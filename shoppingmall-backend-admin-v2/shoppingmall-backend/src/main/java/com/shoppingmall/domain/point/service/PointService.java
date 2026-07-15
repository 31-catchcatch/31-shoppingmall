package com.shoppingmall.domain.point.service;

import com.shoppingmall.domain.point.dto.response.PointHistoryResponse;
import com.shoppingmall.domain.point.entity.PointHistory;
import com.shoppingmall.domain.point.repository.PointHistoryRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 포인트 적립/사용/조정 공통 서비스.
 * - 일반 사용자의 "GET /users/me/points" 조회
 * - 관리자의 "PATCH /admin/users/{userId}/points" 수동 조정
 * 둘 다 이 서비스를 통해서만 포인트를 바꾸도록 해서, point_histories 이력이 항상 같이 남게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /** 포인트 증감 + 이력 기록. amount는 양수(적립/증액)/음수(사용/차감) 모두 가능. */
    @Transactional
    public PointHistoryResponse adjustPoint(Long userId, int amount, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        int newBalance = user.getPoint() + amount;
        if (newBalance < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        user.adjustPoint(amount);

        PointHistory history = PointHistory.builder()
                .user(user)
                .amount(amount)
                .balanceAfter(newBalance)
                .reason(reason)
                .build();

        return PointHistoryResponse.from(pointHistoryRepository.save(history));
    }

    public PageResponse<PointHistoryResponse> getMyPointHistory(Long userId, Pageable pageable) {
        Page<PointHistoryResponse> page = pointHistoryRepository
                .findAllByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(PointHistoryResponse::from);
        return PageResponse.from(page);
    }
}
