package com.shoppingmall.global.security;

import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * [3-2 조치] 계정 단위 로그인 실패 횟수 관리.
 *
 * <p>실패 카운트는 반드시 커밋되어야 한다. 로그인 실패는 예외로 처리되므로
 * 호출 측 트랜잭션이 롤백되면 카운트도 함께 사라진다. 그래서 REQUIRES_NEW 로
 * 독립 트랜잭션에서 기록한다. (PaymentLedgerService 와 같은 이유·같은 패턴)
 *
 * <p>⚠️ REQUIRES_NEW 는 반드시 <b>다른 빈</b>에서 호출해야 한다. 같은 클래스 안에서
 * 호출하면 Spring 프록시를 거치지 않아(self-invocation) 전파 속성이 통째로 무시된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lock-minutes:10}")
    private int lockMinutes;

    private final UserRepository userRepository;

    /** 비밀번호 대조 전에 호출한다. 잠긴 계정이면 예외. */
    public void assertNotLocked(User user) {
        if (user.isLocked()) {
            log.warn("[LOGIN] 잠긴 계정 로그인 시도. userId={}", user.getId());
            throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onFailure(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.recordLoginFailure(maxAttempts, lockMinutes);
            if (user.isLocked()) {
                log.warn("[LOGIN] 실패 {}회 초과로 계정 잠금. userId={}, until={}",
                        maxAttempts, userId, user.getLockedUntil());
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSuccess(Long userId) {
        userRepository.findById(userId).ifPresent(User::recordLoginSuccess);
    }
}
