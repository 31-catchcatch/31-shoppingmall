package com.shoppingmall.global.config;

import com.shoppingmall.global.security.IpRateLimitFilter;
import com.shoppingmall.global.security.jwt.JwtAuthenticationFilter;
import com.shoppingmall.global.security.jwt.JwtExceptionHandlers;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionHandlers.EntryPoint jwtEntryPoint;
    private final JwtExceptionHandlers.DeniedHandler jwtDeniedHandler;
    private final IpRateLimitFilter ipRateLimitFilter;   // [3-2 조치]

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
                // [4-1 판단] 토큰을 Authorization: Bearer 헤더로 전송하는 stateless API 이므로
                // 브라우저가 교차 사이트 요청에 자격 증명을 자동으로 싣지 않아 CSRF 가 성립하지 않는다.
                // ⚠️ 토큰을 HttpOnly 쿠키로 옮기는 순간(4-1 본조치) 반드시 아래처럼 켜야 한다.
                //    .csrf(csrf -> csrf
                //            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                //            .ignoringRequestMatchers("/api/v1/auth/*/login", "/api/v1/auth/*/signup"))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ▼▼▼ [1-1][1-5][5-1] 보안 헤더 ▼▼▼
                // ⚠️ 이 헤더는 Spring 이 생성하는 응답(주로 /api/**, /uploads/**)에만 붙는다.
                //    프론트 HTML(*.html)은 web-01 의 Nginx 가 직접 서빙하므로,
                //    화면단 XSS 를 막는 CSP 는 반드시 Nginx 에도 같이 설정해야 한다. (nginx.conf 참고)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' https://js.tosspayments.com; "
                                        + "connect-src 'self' https://api.tosspayments.com; "
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

                        // 나머지(/users/me/**, /carts/**, /orders/** 등)는 로그인한 사용자만
                        .anyRequest().authenticated()
                )

                // [3-2 조치] IP 단위 요청 제한을 인증 필터보다 앞에 둔다
                .addFilterBefore(ipRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * [1-1][4-1 조치] CORS 오리진 축소.
     *
     * <p>기존 setAllowedOriginPatterns("*") + setAllowCredentials(true) 조합은
     * 임의의 외부 사이트가 인증된 상태로 API 를 호출할 수 있게 만든다.
     * 운영 도메인이 확정되면 아래 목록만 교체하면 된다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://192.168.10.12",     // web-01 (운영 도메인 확정 시 교체)
                "http://localhost:5173",    // 로컬 개발용 (배포 전 제거 권장)
                "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
