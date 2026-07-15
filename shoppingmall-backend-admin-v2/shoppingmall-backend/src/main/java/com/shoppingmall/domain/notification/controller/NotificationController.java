package com.shoppingmall.domain.notification.controller;

import com.shoppingmall.domain.notification.dto.request.NotificationSendRequest;
import com.shoppingmall.domain.notification.service.NotificationService;
import com.shoppingmall.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 실제 서비스 흐름에서는 order/qna 등 다른 도메인이 NotificationService 를 직접 호출하므로
 * 이 엔드포인트는 필수 경로가 아니다. 수동 트리거/테스트 용도로만 열어둠 (로그인 필요, SecurityConfig
 * 기본 규칙 그대로 적용 - 별도 permitAll 추가 안 함).
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
}
