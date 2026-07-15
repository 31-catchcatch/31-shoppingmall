package com.shoppingmall.domain.notification.service;

import com.shoppingmall.domain.notification.entity.Notification;
import com.shoppingmall.domain.notification.entity.NotificationType;
import com.shoppingmall.domain.notification.repository.NotificationRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API 명세서 "공통/인증 - 알림 - 알림 발송 및 관리".
 *
 * 이 API는 프론트가 직접 호출하는 API라기보다, order/payment/qna 같은 다른 도메인 서비스가
 * 상태 변화(배송 시작, 환불 승인, 답변 등록 등) 시점에 이 send() 메서드를 자바 코드로 직접
 * 호출하는 내부 연동 지점이다. NotificationController 는 관리자 강제 발송 등 테스트 용도로만 남겨둠.
 *
 * TODO(실제 연동 필요): 지금은 DB에 이력만 쌓고, 실제 알림톡/문자 발송(카카오 비즈메시지, NHN 등)은
 * 연동 전이라 로그로만 남긴다. 다른 도메인 담당자들은 아래처럼 주입받아 쓰면 됨:
 *
 *   private final NotificationService notificationService;
 *   ...
 *   notificationService.send(userId, NotificationType.DELIVERY, "배송이 시작되었습니다", "...");
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void send(Long userId, NotificationType type, String title, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .build();
        notificationRepository.save(notification);

        // 실제 알림톡/문자 발송 연동 전까지는 로그로 대체
        log.info("[MOCK NOTIFICATION] to userId={} [{}] {} - {}", userId, type, title, content);
    }
}
