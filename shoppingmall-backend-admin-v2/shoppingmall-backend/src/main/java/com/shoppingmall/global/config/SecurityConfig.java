package com.shoppingmall.global.config;

import com.shoppingmall.global.security.IpRateLimitFilter;
import com.shoppingmall.global.security.cookie.AuthCookieProperties;
import com.shoppingmall.global.security.csrf.CsrfCookieFilter;
import com.shoppingmall.global.security.csrf.SpaCsrfTokenRequestHandler;
import com.shoppingmall.global.security.csrf.StatelessCsrfTokenRepository;
import com.shoppingmall.global.security.jwt.JwtAuthenticationFilter;
import com.shoppingmall.global.security.jwt.JwtExceptionHandlers;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
// [4-1 조치] 메인 클래스에 @ConfigurationPropertiesScan 이 없으므로 여기서 명시 등록한다.
@EnableConfigurationProperties(AuthCookieProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionHandlers.EntryPoint jwtEntryPoint;
    private final JwtExceptionHandlers.DeniedHandler jwtDeniedHandler;
    private final IpRateLimitFilter ipRateLimitFilter;   // [3-2 조치]
    private final AuthCookieProperties cookieProperties;  // [4-1 조치] CSRF 쿠키 속성 통일용

    /**
     * [4-1 조치] CSRF 킬스위치. 기본 켜짐.
     *
     * <p>이 프로젝트에는 테스트 소스가 없어, 켠 뒤 예상 못 한 403 이 나오면 되돌릴 수단이
     * 재빌드밖에 없다. 환경변수 {@code APP_SECURITY_CSRF_ENABLED=false} 하나로 끌 수 있게 둔다.
     * (끄더라도 쿠키 인증은 그대로 동작하므로 서비스는 계속된다)
     */
    @Value("${app.security.csrf-enabled:true}")
    private boolean csrfEnabled;

    /**
     * [1-1][4-1 조치] CORS 허용 오리진. application.yml 의 {@code app.cors.allowed-origins}
     * (환경변수 {@code CORS_ALLOWED_ORIGINS})로 주입한다. 운영 도메인이 환경마다 다르므로
     * 하드코딩 대신 설정으로 뺐다 — HTTPS 이관 시 {@code https://catchcatch31.com} 를 넣는다.
     */
    @Value("${app.cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * [배포 조치] @Component 로 선언된 Filter 빈은 Spring Boot 가 서블릿 컨테이너에도
     * 자동 등록한다. 아래 두 필터는 SecurityFilterChain 안에서 addFilterBefore 로
     * 이미 실행되므로, 자동 등록을 꺼두지 않으면 <b>요청당 두 번</b> 실행된다.
     *
     * <p>IpRateLimitFilter 는 카운터가 2씩 증가해 실제 임계값이 절반(20 -> 10)이 되고,
     * JwtAuthenticationFilter 는 SecurityContext 를 중복 설정한다.
     * (외부 Tomcat WAR 배포에서도 동일하게 발생한다)
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<IpRateLimitFilter> disableIpRateLimitFilterAutoRegistration(
            IpRateLimitFilter filter) {
        FilterRegistrationBean<IpRateLimitFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // [4-1 조치] 인증 토큰이 HttpOnly 쿠키로 옮겨갔으므로 CSRF 방어를 켠다.
                //
                //   쿠키는 브라우저가 교차 사이트 요청에도 자동으로 실어 보낸다. 헤더 방식일 때는
                //   성립하지 않던 CSRF 가 쿠키로 옮기는 순간 생기므로, 두 조치는 한 쌍이다.
                //   (SameSite=Lax 가 1차 방어, 이 토큰 검사가 2차 방어다)
                .csrf(csrf -> {
                    if (!csrfEnabled) {
                        csrf.disable();
                        return;
                    }
                    csrf
                            .csrfTokenRepository(csrfTokenRepository())
                            // 쿠키값을 그대로 헤더에 넣는 SPA 방식을 받아들인다 (기본 Xor 핸들러면 전부 403)
                            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                            // ── 로그인 전 POST 는 면제한다 ──────────────────────────────
                            //  화면 HTML 은 Nginx 가 서빙하므로 로그인 전에는 브라우저가 Spring 을
                            //  한 번도 거치지 않는다 → XSRF 쿠키가 없는 상태로 첫 POST 를 보낸다.
                            //  프론트에 프라이밍(GET /auth/csrf)이 있지만, 그게 한 번 실패하면
                            //  로그인이 통째로 막히므로 안전망으로 면제를 둔다.
                            //
                            //  ⚠️ /auth/refresh 와 /auth/user/logout 은 일부러 면제하지 않는다.
                            //     - refresh 는 항상 401 직후에 트리거되고 그 401 응답이 쿠키를 실어 보낸다.
                            //     - logout 도 상태변경이라 CSRF 로그아웃 공격 대상이다.
                            //  ⚠️ /admin/users 는 POST 만 관리자 로그인이고 GET 은 사용자 목록조회다.
                            //     반드시 메서드 단위로 면제해야 한다.
                            .ignoringRequestMatchers(
                                    new AntPathRequestMatcher("/api/v1/auth/user/login", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/seller/login", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/user/signup", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/seller/signup", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/user/verify", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/seller/verify", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/user/find-account", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/seller/find-account", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/find-username", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/verify-account", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/reset-password", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/check-username", "POST"),
                                    new AntPathRequestMatcher("/api/v1/auth/email-verification", "POST"),
                                    new AntPathRequestMatcher("/api/v1/admin/users", "POST")
                            );
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ▼▼▼ [1-1][1-5][5-1] 보안 헤더 ▼▼▼
                // ⚠️ 이 헤더는 Spring 이 생성하는 응답(주로 /api/**, /uploads/**)에만 붙는다.
                //    프론트 HTML(*.html)은 web-01 의 Nginx 가 직접 서빙하므로,
                //    화면단 XSS 를 막는 CSP 는 반드시 Nginx 에도 같이 설정해야 한다. (nginx.conf 참고)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' https://js.tosspayments.com; "
                                        + "connect-src 'self' https://*.tosspayments.com; "
                                        + "frame-src https://*.tosspayments.com; "
                                        + "style-src 'self'; "
                                        + "img-src 'self' data: https:; "
                                        + "object-src 'none'; "
                                        + "base-uri 'self'; "
                                        + "frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(rp -> rp.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        // ⚠️ [5-2] HSTS 는 HTTPS 적용(SYSTEM WEB-20) 이후에 활성화할 것.
                        //    HTTP 환경에서 켜면 접속이 불가능해진다.
                        // .httpStrictTransportSecurity(hsts -> hsts
                        //         .includeSubDomains(true).maxAgeInSeconds(31536000))
                )
                // ▲▲▲ 보안 헤더 ▲▲▲

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(jwtEntryPoint)
                        .accessDeniedHandler(jwtDeniedHandler))

                .authorizeHttpRequests(auth -> auth
                        // 공통/인증 - 토큰 없이 접근 가능 (API 명세서 "공통/인증", "일반 사용자" 중 로그인 전 화면)
                        // [2-1 조치] "/api/v1/files/upload" 를 permitAll 목록에서 제거했다.
                        //            비인증 파일 업로드가 가능해 악성 파일 업로드의 진입점이었다.
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/categories",
                                "/api/v1/search/**",
                                "/api/v1/products", "/api/v1/products/*",
                                "/uploads/**",
                                "/api/v1/products/*/reviews",
                                "/api/v1/banners"
                        ).permitAll()

                        // 브랜드 목록은 비로그인 허용 (브랜드 좋아요 기능은 제거됨)
                        .requestMatchers(HttpMethod.GET, "/api/v1/brands").permitAll()

                        // 토스 결제창 호출에 필요한 공개 설정값(clientKey/리다이렉트 URL) 조회.
                        // 같은 /api/v1/payments 경로의 POST(결제 승인)는 아래 anyRequest().authenticated() 가 그대로 적용된다.
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/config").permitAll()

                        // QnA는 GET(목록조회)만 비로그인 허용, POST(등록)는 로그인 필요
                        // -> 같은 경로를 GET/POST 둘 다 쓰므로 메서드 단위로 분리해야 함
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/*/qna").permitAll()

                        // [2-1 조치] 파일 업로드는 로그인 사용자만 (판매자 상품 등록 / 리뷰 사진)
                        // ⚠️ 로그인 전에 파일을 올리는 화면(판매자 신청 등)이 있다면 프론트 확인 후 조정할 것
                        .requestMatchers(HttpMethod.POST, "/api/v1/files/upload")
                            .hasAnyRole("USER", "SELLER", "ADMIN")

                        // 관리자 로그인(POST /admin/users)은 로그인 전이라 토큰이 없으므로 permitAll.
                        // 같은 /api/v1/admin/users 경로의 GET(사용자 목록조회)은 아래 hasRole("ADMIN") 규칙이 그대로 적용됨.
                        // ⚠️ 이 경로는 애플리케이션 레벨 인증과 별개로, 배포 시 Nginx에서 IP 제한을 반드시 걸 것 (README 참고)
                        .requestMatchers(HttpMethod.POST, "/api/v1/admin/users").permitAll()

                        // 판매자 전용 (API 명세서 "판매자" 도메인)
                        .requestMatchers("/api/v1/seller/**").hasRole("SELLER")

                        // 관리자 전용 (API 명세서 "관리자" 도메인)
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // [4-5 조치] 알림 발송은 요청 본문의 userId 를 그대로 대상으로 삼는다.
                        //            아래 anyRequest().authenticated() 만 적용되던 탓에 로그인한
                        //            일반 사용자가 임의 사용자에게 알림을 보낼 수 있었다(피싱 유포 경로).
                        //            실사용처는 관리자 쿠폰 심사 화면의 판매자 통보뿐이므로 ADMIN 으로 제한한다.
                        //            ⚠️ 다른 도메인 서비스는 NotificationService 를 직접 호출하므로 이 규칙의 영향을 받지 않는다.
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications/send").hasRole("ADMIN")

                        // 나머지(/users/me/**, /carts/**, /orders/** 등)는 로그인한 사용자만
                        .anyRequest().authenticated()
                )

                // [3-2 조치] IP 단위 요청 제한을 인증 필터보다 앞에 둔다
                .addFilterBefore(ipRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // [4-1 조치] CSRF 토큰은 지연 로딩이라 아무도 읽지 않으면 Set-Cookie 가 나가지 않는다.
                //            이 필터가 매 요청 렌더링을 강제해 프론트가 붙일 토큰을 확보하게 한다.
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);

        return http.build();
    }

    /**
     * [4-1 조치] CSRF 토큰 저장소.
     *
     * <p>JS 가 읽어 헤더에 넣어야 하므로 {@code withHttpOnlyFalse()} 다.
     * 이 쿠키는 <b>자격증명이 아니다</b> — 값을 아는 것만으로는 아무 권한도 생기지 않으며,
     * 교차 사이트에서 읽을 수 없다는 점(동일 출처 정책)이 방어의 근거다.
     *
     * <p>secure/sameSite/path 를 <b>명시</b>하는 이유: 기본값이 {@code request.isSecure()} 인데
     * {@code server.forward-headers-strategy} 가 없어 HTTPS 로 이관해도 계속 false 로 남는다.
     * 인증 쿠키와 같은 {@code app.cookie.*} 설정에 묶어 환경변수 하나로 통제한다.
     */
    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(builder -> builder
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path("/"));

        // [4-1 조치 · 3단계] 인증 시점의 토큰 회전을 막는다.
        //   stateless + 매 요청 JWT 인증이라, 인증된 모든 요청이 "새 인증"으로 취급되어
        //   응답마다 XSRF-TOKEN 쿠키가 삭제됐다(실측: refresh 가 204/403 을 번갈아 냄).
        //   자세한 근거는 StatelessCsrfTokenRepository 주석 참고.
        return new StatelessCsrfTokenRepository(repository);
    }

    /**
     * [1-1][4-1 조치] CORS 오리진 축소.
     *
     * <p>기존 setAllowedOriginPatterns("*") + setAllowCredentials(true) 조합은
     * 임의의 외부 사이트가 인증된 상태로 API 를 호출할 수 있게 만든다.
     * 운영 도메인이 확정되면 환경변수 {@code CORS_ALLOWED_ORIGINS} 만 교체하면 된다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // [4-1 조치] Authorization 헤더 인증을 제거했으므로 허용 목록에서도 뺀다.
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
