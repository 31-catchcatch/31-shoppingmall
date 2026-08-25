package com.shoppingmall.global.security.csrf;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * [4-1 조치] CSRF 토큰을 매 요청마다 강제로 렌더링해 {@code XSRF-TOKEN} 쿠키가 나가게 한다.
 *
 * <p><b>왜 필요한가</b> — Spring 의 CSRF 토큰은 <b>지연 로딩</b>이다.
 * 요청 처리 중 아무도 {@link CsrfToken#getToken()} 을 호출하지 않으면
 * {@code Set-Cookie: XSRF-TOKEN} 이 <b>아예 나가지 않는다</b>.
 * 그러면 프론트는 붙일 토큰이 없어 모든 상태변경 요청이 <b>첫 시도에 403</b> 이 된다.
 * 이 프로젝트에서는 그 403 이 관리자 화면에서 특히 치명적이었다(예전 코드가 403 을
 * 로그아웃으로 처리했다).
 *
 * <p>{@code getToken()} 을 한 번 부르는 것만으로 렌더링이 강제된다.
 *
 * <p><b>부작용(기록)</b> — 모든 Spring 응답에 {@code Set-Cookie} 가 붙는다.
 * 현재 web-01 의 Nginx 는 {@code /api/v1/} 을 캐시하지 않으므로 무해하지만,
 * CDN·프록시 캐시를 도입하면 캐시 포이즈닝 벡터가 된다. <b>API 응답 캐시는 계속 꺼둘 것.</b>
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();   // 렌더링 강제 → Set-Cookie 발생
        }
        filterChain.doFilter(request, response);
    }
}
