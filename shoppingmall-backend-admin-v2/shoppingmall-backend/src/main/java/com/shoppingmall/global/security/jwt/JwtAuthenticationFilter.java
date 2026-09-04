package com.shoppingmall.global.security.jwt;

import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.security.CustomUserDetails;
import com.shoppingmall.global.security.cookie.AuthCookieFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * 권한이 필요한 API 를 <b>HttpOnly 인증 쿠키(CC_AT)의 JWT</b> 로 식별한다.
 * 인증이 필요 없는 API(상품 리스트, 로그인/회원가입 등)는 SecurityConfig 에서 permitAll 처리하고
 * 이 필터는 통과만 시킨다.
 *
 * <p>[4-1 조치 · 3단계] {@code Authorization: Bearer} 헤더 인식을 <b>제거했다</b>.
 * 토큰이 JS 가 읽을 수 있는 곳에 있으면 XSS 한 번으로 탈취되므로, 브라우저만 다룰 수 있는
 * HttpOnly 쿠키로 일원화했다. 쿠키는 교차 사이트 요청에도 자동 전송되므로
 * CSRF 방어(SecurityConfig 의 csrf 설정)와 <b>반드시 한 쌍</b>으로 동작해야 한다.
 *
 * principal은 CustomUserDetails로 감싸서 넣는다 (Long userId를 그대로 넣지 않음).
 * 컨트롤러에서 @AuthenticationPrincipal CustomUserDetails userDetails 로 받아
 * userDetails.getUser().getId() 형태로 꺼내 쓰는 게 이 프로젝트의 표준 패턴이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuthCookieFactory authCookieFactory;   // [4-1 조치] 쿠키에서 토큰 추출
    private final TokenInvalidationRegistry tokenInvalidationRegistry;   // [4-2 조치]

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            Long userId = jwtTokenProvider.getUserId(token);

            // [4-2 조치] 로그아웃 이후 발급분인지 먼저 확인한다. 로그아웃은 Refresh 만 지워서
            // 이미 발급된 Access Token 이 남은 유효기간 동안 계속 통했다(탈취 시 사용자가
            // 끊을 방법이 없었다). 판정은 TokenInvalidationRegistry 가 메모리로 들고 있으므로
            // DB 를 건드리지 않으며, 무효 토큰은 사용자 조회 전에 걷어낸다.
            if (tokenInvalidationRegistry.isInvalidated(userId, jwtTokenProvider.getIssuedAt(token))) {
                log.debug("무효화된 토큰 사용 시도. userId={}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            Optional<User> userOptional = userRepository.findById(userId);
            // 탈퇴(soft delete, 관리자 정지 처리도 동일 플래그 재사용) 등으로 토큰은 유효하지만
            // 사용자가 사라진 경우 인증 미적용 -> 401 처리됨.
            // (매 요청마다 DB를 다시 조회하므로, 관리자가 정지 처리하면 기존 액세스 토큰도 즉시 무효화된다)
            if (userOptional.isPresent() && !userOptional.get().isDeleted()) {
                CustomUserDetails userDetails = new CustomUserDetails(userOptional.get());
                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * [4-1 조치] 인증 토큰을 쿠키에서 먼저 찾고, 없으면 Authorization 헤더로 넘어간다.
     *
     * <p><b>쿠키를 먼저 보는 이유</b>: 프론트의 fetch 래퍼는 localStorage 에 토큰이 남아 있으면
     * 무조건 Authorization 헤더를 붙인다. 헤더를 먼저 보면 사용자가 브라우저 저장소를 비울 때까지
     * 묵은 토큰이 새 쿠키를 계속 가려 전환이 끝나지 않는다.
     *
     * <p><b>"검증 통과한 첫 번째"를 쓰는 이유</b>: 단순히 쿠키가 있으면 쿠키만 쓰는 방식이면
     * 만료된 쿠키가 유효한 헤더 토큰을 가려 로그인이 끊긴다. 전환기(헤더·쿠키 공존) 동안의
     * 장애 경로를 없애기 위해 후보를 순서대로 검증한다.
     *
     * <p>전환이 끝나면(3단계) 헤더 분기를 제거한다.
     */
    /** [4-1 조치 · 3단계] 인증 쿠키만 본다. Authorization 헤더 경로는 제거됐다. */
    private String resolveToken(HttpServletRequest request) {
        String candidate = fromCookie(request);
        if (!StringUtils.hasText(candidate) || !jwtTokenProvider.validateToken(candidate)) {
            return null;
        }

        // [H-2 조치] Refresh Token 을 액세스 쿠키 자리에 넣은 요청을 거부한다.
        // 서명·만료만 검사하던 탓에 7일짜리 토큰이 액세스 토큰으로 통했고,
        // 무효화 레지스트리 수명(15분)이 지나면 로그아웃한 계정에서도 다시 통했다.
        if (jwtTokenProvider.isRefreshToken(candidate)) {
            log.debug("리프레시 토큰이 액세스 쿠키로 전달됨. 인증 미적용");
            return null;
        }

        return candidate;
    }

    private String fromCookie(HttpServletRequest request) {
        return authCookieFactory.readAccessToken(request).orElse(null);
    }

}
