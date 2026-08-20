package com.shoppingmall.domain.admin.dto.request;

import com.shoppingmall.global.validation.NoHtml;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 관리자 배너 생성/수정 공용 요청 DTO. (POST · PUT /admin/banners)
 *
 * <p>startAt/endAt 은 타임존 없는 벽시계 시각이다. 프론트의 input[type=datetime-local] 값이
 * 그대로 올라오며("2026-08-01T00:00"), 값이 없으면 빈 문자열이 아니라 null 로 온다.
 *
 * <p>imageUrl/linkUrl 의 스킴 검증은 {@code AdminBannerService.assertSafeUrl} 에서 수행한다
 * — 정규식 한 줄로 표현하면 읽기 어렵고, 거부 사유를 구분해 안내할 수 없기 때문이다.
 */
public record AdminBannerRequest(

        @NotBlank(message = "배너명을 입력해 주세요.")
        @Size(max = 100, message = "배너명은 100자 이하여야 합니다.")
        @NoHtml   // [1-1] 배너명은 그대로 화면에 노출되므로 태그를 허용하지 않는다
        String title,

        @NotBlank(message = "배너 이미지 주소를 입력해 주세요.")
        @Size(max = 512, message = "이미지 주소는 512자 이하여야 합니다.")
        String imageUrl,

        @Size(max = 512, message = "연결 링크는 512자 이하여야 합니다.")
        String linkUrl,

        // 노출 순서는 1부터 시작한다 (관리자 화면 표기와 DB 값을 일치시키기 위함).
        // 기존 0번 데이터는 V16 마이그레이션에서 1..n 으로 다시 매긴다.
        @NotNull(message = "노출 순서를 입력해 주세요.")
        @Positive(message = "노출 순서는 1 이상이어야 합니다.")
        Integer sortOrder,

        // Boolean(래퍼)으로 받는다. primitive 로 두면 요청에서 빠졌을 때 조용히 false 가 되어
        // "노출 사용" 체크가 의도치 않게 꺼진 채 저장된다.
        @NotNull(message = "노출 여부를 지정해 주세요.")
        Boolean active,

        LocalDateTime startAt,

        LocalDateTime endAt

) {
}
