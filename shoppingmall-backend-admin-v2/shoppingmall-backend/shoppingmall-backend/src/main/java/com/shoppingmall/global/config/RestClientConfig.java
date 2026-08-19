package com.shoppingmall.global.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 외부 HTTP 호출용 RestClient 설정.
 *
 * 이 프로젝트에서 외부 API를 호출하는 것은 토스페이먼츠 결제 승인이 처음이다.
 * webflux(WebClient)나 httpclient5 의존성이 없으므로 spring-boot-starter-web 만으로 쓸 수 있는
 * RestClient(Spring 6.1+)를 사용한다. 요청 팩토리는 JDK 내장 HttpClient 가 선택된다.
 *
 * 타임아웃을 반드시 지정하는 이유: 기본값은 무제한이라 PG가 응답하지 않으면 was 스레드가
 * 영원히 물려 있게 된다. 결제 승인은 사용자가 결제창에서 돌아온 뒤 10분 안에 끝나야 하는 호출이라
 * 빨리 실패하고 실패로 기록하는 편이 안전하다.
 */
@Configuration
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Spring Boot 가 자동 구성하는 RestClient.Builder 를 이 빈으로 대체한다(@ConditionalOnMissingBean).
     * 자동 구성 기본값에는 타임아웃이 없기 때문이다.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings));
    }
}
