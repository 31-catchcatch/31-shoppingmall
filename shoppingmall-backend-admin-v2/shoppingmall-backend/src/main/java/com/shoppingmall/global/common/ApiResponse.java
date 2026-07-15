package com.shoppingmall.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 모든 API 응답의 공통 포맷.
 * 프론트 파트와 "성공/실패 구분 + data 필드 위치"를 미리 맞춰두면
 * 이후 도메인이 늘어나도 응답 스펙 논의를 반복할 필요가 없다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
