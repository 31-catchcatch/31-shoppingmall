package com.shoppingmall.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 관리자 수동 포인트 조정. amount는 양수(지급)/음수(회수) 모두 가능. */
public record PointAdjustRequest(
        @NotNull Integer amount,
        @NotBlank(message = "조정 사유를 입력해주세요.") String reason
) {
}
