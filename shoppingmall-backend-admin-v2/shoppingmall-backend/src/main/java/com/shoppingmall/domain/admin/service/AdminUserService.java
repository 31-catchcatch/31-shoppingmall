package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.request.PointAdjustRequest;
import com.shoppingmall.domain.admin.dto.response.AdminUserResponse;
import com.shoppingmall.domain.point.dto.response.PointHistoryResponse;
import com.shoppingmall.domain.point.service.PointService;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.common.PageResponse;
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
}
