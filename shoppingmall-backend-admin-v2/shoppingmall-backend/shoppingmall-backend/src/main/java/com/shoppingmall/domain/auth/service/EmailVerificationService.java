package com.shoppingmall.domain.auth.service;

import com.shoppingmall.domain.auth.dto.request.EmailVerificationRequest;
import com.shoppingmall.domain.auth.dto.response.EmailVerificationResponse;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * POST /api/v1/auth/email-verification 담당.
 *
 * TODO(실제 연동 필요): 지금은 실제 메일 발송 업체(AWS SES, 네이버클라우드 등) 연동 전이라
 * 인증코드를 실제로 보내지 않고 서버 로그에만 출력하는 mock 구현이다.
 * 코드 저장도 짧은 TTL이라 DB 테이블 없이 인메모리(ConcurrentHashMap)로 처리했다.
 * was-01 인스턴스가 여러 대로 늘어나면(로드밸런싱) 이 방식은 깨지므로 그때는 Redis로 교체 필요.
 */
@Slf4j
@Service
public class EmailVerificationService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private record CodeEntry(String code, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, CodeEntry> codeStore = new ConcurrentHashMap<>();

    public EmailVerificationResponse handle(EmailVerificationRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            return sendCode(request.email());
        }
        return verifyCode(request.email(), request.code());
    }

    private EmailVerificationResponse sendCode(String email) {
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
        codeStore.put(email, new CodeEntry(code, Instant.now().plus(CODE_TTL)));

        // 실제 메일 발송 연동 전까지는 로그로 대체 (로컬/개발 환경에서 콘솔 확인용)
        log.info("[MOCK EMAIL] {} 에게 인증코드 발송: {}", email, code);

        return EmailVerificationResponse.ofSent();
    }

    private EmailVerificationResponse verifyCode(String email, String code) {
        CodeEntry entry = codeStore.get(email);
        if (entry == null || entry.isExpired()) {
            codeStore.remove(email);
            throw new CustomException(ErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!entry.code().equals(code)) {
            throw new CustomException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        codeStore.remove(email);
        return EmailVerificationResponse.ofVerified();
    }
}
