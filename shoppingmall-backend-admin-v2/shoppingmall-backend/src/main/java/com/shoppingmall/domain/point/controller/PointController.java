package com.shoppingmall.domain.point.controller;

import com.shoppingmall.domain.point.dto.response.PointHistoryResponse;
import com.shoppingmall.domain.point.service.PointService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API 명세서 "일반 사용자 - 마이페이지 - 포인트 보유 및 사용내역 조회" */
@RestController
@RequestMapping("/api/v1/users/me/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PointHistoryResponse>>> getMyPoints(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        PageResponse<PointHistoryResponse> response =
                pointService.getMyPointHistory(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
