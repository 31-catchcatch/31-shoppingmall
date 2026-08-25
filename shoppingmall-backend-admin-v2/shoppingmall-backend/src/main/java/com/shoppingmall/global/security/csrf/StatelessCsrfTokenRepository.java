package com.shoppingmall.global.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * [4-1 조치 · 3단계] 인증 시점의 CSRF 토큰 <b>회전을 막는</b> 저장소 래퍼.
 *
 * <p><b>어떤 문제를 푸는가</b> — Spring Security 는 인증이 새로 성립할 때
 * {@code CsrfAuthenticationStrategy} 를 돌려 기존 CSRF 토큰을 지우고 새로 발급한다.
 * 세션 고정(session fixation) 공격을 막기 위한 동작이다.
 *
 * <p>그런데 이 프로젝트는 <b>stateless</b> 이고, 매 요청마다 {@code JwtAuthenticationFilter} 가
 * 쿠키의 JWT 로 인증을 새로 심는다. 그래서 <b>인증된 모든 요청이 "새 인증"으로 취급</b>되어
 * 응답마다 {@code Set-Cookie: XSRF-TOKEN=; Max-Age=0} 이 나갔다.
 * 삭제 뒤 새 토큰은 아무도 렌더링하지 않으므로 쿠키가 그대로 사라지고,
 * 그 다음 상태변경 요청이 403 이 된다. 실측하면 <b>204 → 403 → 204 → 403</b> 이 번갈아 나온다.
 *
 * <p><b>왜 회전을 버려도 되는가</b> — 회전의 목적은 세션 고정 방지인데,
 * 이 프로젝트에는 고정시킬 서버 세션이 없다(STATELESS). CSRF 토큰은 자격증명이 아니라
 * "요청이 우리 화면에서 출발했는가"를 확인하는 값이며, 교차 사이트에서 읽을 수 없다는 점
 * (동일 출처 정책)이 방어의 근거다. 따라서 요청마다 갈아끼울 필요가 없다.
 *
 * <p>삭제(=null 저장) 요청만 무시하고 나머지는 그대로 위임한다.
 */
public class StatelessCsrfTokenRepository implements CsrfTokenRepository {

    private final CsrfTokenRepository delegate;

    public StatelessCsrfTokenRepository(CsrfTokenRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegate.generateToken(request);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        // token == null 은 "쿠키를 지워라" 라는 뜻이다. 인증 때마다 들어오므로 무시한다.
        if (token == null) {
            return;
        }
        delegate.saveToken(token, request, response);
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegate.loadToken(request);
    }
}
