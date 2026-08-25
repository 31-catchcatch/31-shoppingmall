package com.shoppingmall.global.security.cookie;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * [4-1][5-1 조치] 인증 쿠키 설정.
 *
 * <p>인증 토큰을 localStorage 대신 HttpOnly 쿠키로 내려보내기 위한 값들이다.
 * 토큰이 JavaScript 로 읽히지 않게 되어 XSS 가 발생해도 탈취가 성립하지 않는다.
 *
 * <p><b>secure 를 설정으로 뺀 이유</b>: 현재 web-01 은 HTTP(80)만 서비스하는데
 * {@code Secure} 쿠키는 HTTPS 에서만 전송되므로, 켜두면 로그인이 통째로 깨진다.
 * 반대로 코드에 false 로 박아두면 HTTPS 로 이관한 뒤에도 평문 구간에 쿠키가 노출된다.
 * 그래서 <b>기본값을 true 로 두고 현재 환경에서만 환경변수로 끄는</b> 방식을 쓴다.
 * (클라우드 이관 후에는 APP_COOKIE_SECURE 를 지우기만 하면 자동으로 Secure 가 붙는다)
 */
@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cookie")
public class AuthCookieProperties {

    /** 액세스 토큰 쿠키 이름. */
    private String accessName = "CC_AT";

    /** 리프레시 토큰 쿠키 이름. */
    private String refreshName = "CC_RT";

    /**
     * HTTPS 로만 쿠키를 전송할지 여부.
     * 기본 true — 설정을 빠뜨렸을 때 안전한 쪽(전송 제한)으로 실패하게 한다.
     */
    private boolean secure = true;

    /**
     * SameSite 속성. 기본 Lax.
     *
     * <p>Strict 는 외부 링크(메신저·메일·검색결과)로 들어온 사용자가 첫 화면에서
     * 비로그인으로 보이는 문제가 있어 쇼핑몰에는 맞지 않는다. Lax 도 교차 사이트의
     * POST·PUT·DELETE 는 그대로 차단하므로 CSRF 관점에서 잃는 것이 없다.
     * None 은 Secure 가 필수라 현재 HTTP 환경에서는 브라우저가 쿠키 자체를 거부한다.
     */
    private String sameSite = "Lax";

    /** 액세스 쿠키 Path. 모든 API 요청에 실려야 하므로 루트. */
    private String path = "/";

    /**
     * 리프레시 쿠키 Path.
     *
     * <p>재발급·로그아웃에서만 필요하므로 범위를 좁혀 둔다. 상품 조회나 이미지 업로드 같은
     * 일반 요청에 7일짜리 자격증명이 함께 실려 나가지 않게 하기 위함이다.
     */
    private String refreshPath = "/api/v1/auth";

    /**
     * 기동 시 유효 설정을 남긴다.
     *
     * <p>secure=true 인 채로 HTTP 환경에 배포되면 브라우저가 Set-Cookie 를
     * <b>조용히 폐기</b>한다. 서버 응답 헤더에는 정상적으로 찍히기 때문에 로그가 없으면
     * 원인을 찾기 매우 어렵다.
     */
    @PostConstruct
    void logEffectiveSettings() {
        log.info("[AUTH-COOKIE] access={}, refresh={}, secure={}, sameSite={}, path={}, refreshPath={}",
                accessName, refreshName, secure, sameSite, path, refreshPath);
        if (secure) {
            log.info("[AUTH-COOKIE] secure=true 입니다. HTTP 로 서비스 중이라면 브라우저가 인증 쿠키를 "
                    + "저장하지 않습니다. HTTPS 적용 전까지는 APP_COOKIE_SECURE=false 로 두십시오.");
        }
    }
}
