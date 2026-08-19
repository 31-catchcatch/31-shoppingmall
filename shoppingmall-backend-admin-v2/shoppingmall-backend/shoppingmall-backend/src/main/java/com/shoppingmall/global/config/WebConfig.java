package com.shoppingmall.global.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * FileStorageService 가 was-01 로컬 디스크에 저장한 파일을 /uploads/** 경로로 그대로 서빙.
 * (실제로는 Nginx가 정적 파일을 더 잘 서빙하지만, 지금은 별도 볼륨 공유 설정 전이라
 *  Spring Boot가 우선 서빙하고, 나중에 Nginx location 설정으로 옮겨도 URL 규칙은 안 바뀜)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadDir.replaceAll("/+$", "") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    /**
     * [2-1 조치] 업로드 파일 응답을 브라우저가 실행하지 못하게 한다.
     *
     * <ul>
     *   <li>X-Content-Type-Options: nosniff — 확장자와 다른 타입으로 재해석 금지</li>
     *   <li>Content-Security-Policy: sandbox — 스크립트 실행 차단</li>
     * </ul>
     *
     * <p>⚠️ Content-Disposition: attachment 는 의도적으로 넣지 않았다.
     * 붙이면 &lt;img src="/uploads/..."&gt; 로 상품 이미지가 표시되지 않아 서비스가 깨진다.
     * 근본 해결은 업로드 파일을 별도 도메인(S3/CloudFront)에서 서빙하는 것이며,
     * 클라우드 전환 시 장기 과제로 다룬다.
     */
    @Bean
    public FilterRegistrationBean<Filter> uploadResponseHardeningFilter() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter((request, response, chain) -> {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setHeader("X-Content-Type-Options", "nosniff");
            res.setHeader("Content-Security-Policy", "sandbox; default-src 'none'");
            chain.doFilter(request, response);
        });
        bean.addUrlPatterns("/uploads/*");
        bean.setOrder(1);
        return bean;
    }
}
