package com.shoppingmall.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [3-2 조치] 인증 계열 엔드포인트에 IP 단위 요청 제한을 건다.
 *
 * <p>계정 잠금(LoginAttemptService)만으로는 계정을 바꿔가며 시도하는
 * 크리덴셜 스터핑을 막을 수 없어 함께 적용한다.
 *
 * <p>⚠️ X-Forwarded-For 를 신뢰하므로 Nginx 에서 반드시 이 헤더를
 * {@code proxy_set_header X-Forwarded-For $remote_addr;} 로 <b>덮어써야</b> 한다.
 * ($proxy_add_x_forwarded_for 는 클라이언트 값에 덧붙이는 방식이라 위조 우회가 가능하다)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_PER_MINUTE = 20;

    /** 무차별 대입 대상이 되는 경로만 건다. 일반 API 는 제외. */
    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/v1/auth/user/login",
            "/api/v1/auth/seller/login",
            "/api/v1/auth/find-username",
            "/api/v1/auth/verify-account",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/user/find-account",
            "/api/v1/auth/seller/find-account",
            "/api/v1/admin/users");

    private final Cache<String, AtomicInteger> counters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(100_000)
            .build();

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !PROTECTED_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        String key = ip + "|" + request.getRequestURI();
        int count = counters.get(key, k -> new AtomicInteger()).incrementAndGet();

        if (count > MAX_PER_MINUTE) {
            log.warn("[RATE-LIMIT] IP 요청 제한 초과. ip={}, path={}, count={}",
                    ip, request.getRequestURI(), count);
            writeTooManyRequests(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.TOO_MANY_REQUESTS.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(ErrorCode.TOO_MANY_REQUESTS.getMessage())));
    }
}
