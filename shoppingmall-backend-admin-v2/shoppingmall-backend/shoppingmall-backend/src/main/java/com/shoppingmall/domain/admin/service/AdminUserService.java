package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.request.PointAdjustRequest;
import com.shoppingmall.domain.admin.dto.request.UserStatusUpdateRequest;
import com.shoppingmall.domain.admin.dto.response.AdminUserResponse;
import com.shoppingmall.domain.auth.repository.RefreshTokenRepository;
import com.shoppingmall.domain.point.dto.response.PointHistoryResponse;
import com.shoppingmall.domain.point.service.PointService;
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
 * API 명세서 "관리자 - 운영 - 사용자" 담당.
 * - GET   /admin/users         (전체 사용자 목록)
 * - GET   /admin/users/points  (사용자별 포인트 현황 목록)
 * - PATCH /admin/users/{userId}/points (포인트 수동 조정)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final PointService pointService;
    private final RefreshTokenRepository refreshTokenRepository;

    public PageResponse<AdminUserResponse> getUsers(Pageable pageable) {
        Page<AdminUserResponse> page = userRepository.findAll(pageable).map(AdminUserResponse::from);
        return PageResponse.from(page);
    }

    /** 포인트 현황 목록도 같은 사용자 목록에 포인트 필드가 이미 포함돼 있어 동일 응답을 재사용 */
    public PageResponse<AdminUserResponse> getUserPoints(Pageable pageable) {
        return getUsers(pageable);
    }

    @Transactional
    public PointHistoryResponse adjustUserPoint(Long userId, PointAdjustRequest request) {
        String reason = "[관리자 조정] " + request.reason();
        return pointService.adjustPoint(userId, request.amount(), reason);
    }

    /**
     * PATCH /admin/users/{userId}/status - 계정 정지/정지해제.
     * 별도 정지 플래그 없이 기존 is_deleted(soft delete)를 그대로 재사용한다.
     * 정지 처리 시 이미 발급된 리프레시 토큰을 전부 삭제해서, 액세스 토큰이
     * (매 요청마다 재검증되어) 즉시 막힌 뒤에도 몰래 재발급받아 우회하지 못하게 한다.
     */
    @Transactional
    public void updateUserStatus(Long userId, UserStatusUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (request.suspended()) {
            user.softDelete();
            refreshTokenRepository.deleteAllByUser(user);
        } else {
            user.restore();
        }
    }
}
