package com.shoppingmall.domain.notification.controller;

import com.shoppingmall.domain.notification.dto.request.NotificationSendRequest;
import com.shoppingmall.domain.notification.dto.response.NotificationResponse;
import com.shoppingmall.domain.notification.service.NotificationService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * /send 는 다른 도메인 서비스가 NotificationService 를 직접 호출하는 게 기본 경로이고,
 * 이 컨트롤러의 /send 는 관리자 화면(쿠폰 심사 결과 통보)에서 쓰는 수동 트리거다.
 *
 * <p><b>[4-5 조치]</b> 대상 사용자를 요청 본문의 userId 로 지정하는 구조라, 로그인만 하면
 * 누구나 임의 사용자에게 알림을 보낼 수 있었다(피싱 유포 경로). SecurityConfig 에서
 * 이 경로만 ADMIN 으로 제한한다 — 권한 규칙을 한 곳에서 보게 하려고 어노테이션 대신
 * SecurityConfig 에 두었다.
 *
 * GET(목록)/PATCH(읽음처리)는 API 명세서 "일반 사용자 - 마이페이지" 매핑 (로그인 필요, SecurityConfig 기본 규칙 적용).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@Valid @RequestBody NotificationSendRequest request) {
        notificationService.send(request.userId(), request.type(), request.title(), request.content());
        return ResponseEntity.ok(ApiResponse.success("알림이 발송되었습니다.", null));
    }

    /** GET /api/v1/notifications - 내 알림 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationResponse> response =
                notificationService.getMyNotifications(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    /** PATCH /api/v1/notifications/{notificationId}/read - 알림 읽음 처리 */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        notificationService.markAsRead(userDetails.getUser().getId(), notificationId);
        return ResponseEntity.ok(ApiResponse.success("알림을 읽음 처리했습니다.", null));
    }
}
