package com.shoppingmall.global.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [1-6 조치] 요청 본문 역직렬화 정책.
 *
 * <p>기본값에서는 {"quantity": 2.9} 가 Integer 2 로 조용히 절삭되어
 * Bean Validation 이 원본 값을 볼 기회조차 없다. 아래 기능을 꺼서
 * 형식이 어긋난 요청은 역직렬화 단계에서 400 으로 거부되게 한다.
 * (400 변환은 GlobalExceptionHandler.handleNotReadable 이 담당)
 *
 * <p>⚠️ FAIL_ON_UNKNOWN_PROPERTIES 는 의도적으로 켜지 않았다.
 * 프론트가 DTO 에 없는 필드를 하나라도 보내면 그 순간 해당 API 가 전부 400 이 된다.
 * 보고서 대응방안의 "화이트리스트 정의"에 해당하므로, 프론트 요청 본문과 DTO 필드를
 * 전수 대조한 뒤 아래 주석을 해제할 것.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonStrictNumberCustomizer() {
        return builder -> builder
                // 2.9 -> 2 절삭 금지 (정수 필드에 실수를 보내면 예외)
                .featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                // "" -> null 변환 금지 (빈 문자열로 객체 필드를 비우는 우회 차단)
                .featuresToDisable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        // TODO(프론트 전수 대조 후 활성화):
        //        .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
