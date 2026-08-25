package com.shoppingmall.global.security.cookie;

import com.shoppingmall.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * [4-1][5-1 조치] 인증 토큰을 HttpOnly 쿠키로 발급·회수한다.
 *
 * <p>종전에는 로그인 응답 본문으로 토큰을 내려 프론트가 localStorage 에 저장했다.
 * 그 구조에서는 XSS 가 한 번만 성립해도 토큰이 그대로 유출된다(최초 진단에서 실증).
 * 쿠키에 HttpOnly 를 붙이면 document.cookie 로 읽을 수 없어 탈취 자체가 불가능해진다.
 *
 * <p>구현상 주의 두 가지:
 * <ul>
 *   <li>{@link ResponseCookie} 를 쓴다 — 서블릿 {@link Cookie} 에는 SameSite setter 가 없다.</li>
 *   <li>반드시 {@code addHeader} 로 붙인다 — {@code setHeader} 를 쓰면 먼저 넣은
 *       Set-Cookie 가 지워져 액세스·리프레시 중 하나만 발급된다.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AuthCookieFactory {

    private final AuthCookieProperties properties;
    private final JwtTokenProvider jwtTokenProvider;

    /** 로그인·재발급 성공 시 두 쿠키를 함께 발급한다. */
    public void writeLoginCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        addCookie(response, buildAccessCookie(accessToken));
        addCookie(response, buildRefreshCookie(refreshToken));
    }

    /**
     * 로그아웃 시 두 쿠키를 만료시킨다.
     *
     * <p>브라우저는 <b>Set-Cookie 의 Path 로 대상 쿠키를 찾는다.</b> 발급할 때와 다른 Path 로
     * 만료 쿠키를 내리면 원래 쿠키가 그대로 남는다. 리프레시 쿠키의 Path 가
     * {@code /api/v1/auth} 인데 로그아웃에서 {@code /} 로 지우면 7일짜리 자격증명이
     * 살아남으므로, 발급과 동일한 Path 를 쓴다.
     */
    public void clearAuthCookies(HttpServletResponse response) {
        addCookie(response, expiredCookie(properties.getAccessName(), properties.getPath()));
        addCookie(response, expiredCookie(properties.getRefreshName(), properties.getRefreshPath()));
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {
        return readCookie(request, properties.getAccessName());
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return readCookie(request, properties.getRefreshName());
    }

    private ResponseCookie buildAccessCookie(String token) {
        return baseCookie(properties.getAccessName(), token, properties.getPath())
                .maxAge(Duration.ofMillis(jwtTokenProvider.getAccessTokenValidityMs()))
                .build();
    }

    private ResponseCookie buildRefreshCookie(String token) {
        return baseCookie(properties.getRefreshName(), token, properties.getRefreshPath())
                .maxAge(Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs()))
                .build();
    }

    private ResponseCookie expiredCookie(String name, String path) {
        return baseCookie(name, "", path).maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)                       // JS 접근 차단 — 이 조치의 핵심
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(path);
    }

    private void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
