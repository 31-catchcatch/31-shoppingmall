package com.shoppingmall.global.security.csrf;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * [4-1 조치] 쿠키에 담긴 CSRF 토큰을 헤더로 그대로 돌려보내는 SPA 방식에 맞춘 핸들러.
 *
 * <p><b>왜 필요한가</b> — Spring Security 6 의 기본 핸들러는
 * {@link XorCsrfTokenRequestAttributeHandler} 다. 이 핸들러는 쿠키에는 raw 값을 담고
 * 요청에서는 <b>BREACH 대응으로 XOR 마스킹된 값</b>을 받기를 기대한다.
 * 그런데 우리 프론트({@code js/http.js})는 {@code XSRF-TOKEN} 쿠키값을 읽어
 * {@code X-XSRF-TOKEN} 헤더에 <b>그대로</b> 넣는다.
 * 기본 핸들러를 그대로 쓰면 모든 비-GET 요청이 403 이 된다
 * ("CSRF 를 켰더니 전부 깨졌다" 의 표준 원인).
 *
 * <p><b>어떻게 해결하나</b> — 응답 쪽({@code handle})은 기본 동작(Xor)에 위임해
 * BREACH 대응을 유지하고, 요청에서 값을 꺼내는 쪽({@code resolveCsrfTokenValue})만
 * 갈라놓는다.
 *
 * <ul>
 *   <li>헤더로 왔다 → JS 가 쿠키값을 그대로 넣은 것이므로 <b>plain</b> 으로 취급</li>
 *   <li>헤더가 없다(폼 파라미터 등) → 기존대로 <b>Xor</b> 로 해석</li>
 * </ul>
 *
 * <p>이 프로젝트에는 네이티브 {@code <form method="post">} 가 없어 실제로는 헤더 경로만
 * 타지만, 폼 경로를 남겨 두어야 나중에 폼이 추가돼도 깨지지 않는다.
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       Supplier<CsrfToken> csrfToken) {
        // 응답(쿠키 발급) 경로는 기본 동작을 그대로 쓴다.
        xor.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        return StringUtils.hasText(headerValue)
                ? plain.resolveCsrfTokenValue(request, csrfToken)
                : xor.resolveCsrfTokenValue(request, csrfToken);
    }
}
