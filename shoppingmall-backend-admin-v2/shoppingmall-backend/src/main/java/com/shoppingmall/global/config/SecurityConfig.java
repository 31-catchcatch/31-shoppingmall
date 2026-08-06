package com.shoppingmall.global.config;

import com.shoppingmall.global.security.jwt.JwtAuthenticationFilter;
import com.shoppingmall.global.security.jwt.JwtExceptionHandlers;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // JWT 기반 stateless API 이므로 CSRF 불필요
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(jwtEntryPoint)
                        .accessDeniedHandler(jwtDeniedHandler))

                .authorizeHttpRequests(auth -> auth
                        // 공통/인증 - 토큰 없이 접근 가능 (API 명세서 "공통/인증", "일반 사용자" 중 로그인 전 화면)
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/files/upload",
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

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 프론트가 별도 오리진(React dev server 등)에서 붙는 경우를 대비한 기본 CORS 설정.
     * 실제 배포 도메인이 정해지면 allowedOrigins 를 좁혀야 한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
