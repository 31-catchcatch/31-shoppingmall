package com.shoppingmall.global.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 상품 리스트, 주문 내역 등 페이징이 필요한 모든 GET 리스트 API 공통 응답.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
