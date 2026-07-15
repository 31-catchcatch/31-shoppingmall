package com.shoppingmall.global.config;

import org.springframework.beans.factory.annotation.Value;
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
}
