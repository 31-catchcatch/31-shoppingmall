package com.shoppingmall.domain.product.dto.response;

import com.shoppingmall.domain.product.entity.Category;

import java.util.List;

/** GET /api/v1/categories - 재귀 트리 구조 응답 */
public record CategoryResponse(
        Long categoryId,
        String name,
        List<CategoryResponse> children
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getChildren().stream().map(CategoryResponse::from).toList()
        );
    }
}
