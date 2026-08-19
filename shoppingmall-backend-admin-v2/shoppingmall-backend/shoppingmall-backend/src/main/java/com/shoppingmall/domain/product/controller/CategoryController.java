package com.shoppingmall.domain.product.controller;

import com.shoppingmall.domain.product.dto.response.CategoryResponse;
import com.shoppingmall.domain.product.service.CategoryService;
import com.shoppingmall.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API 명세서 "공통/인증 - 상품 - 카테고리 목록 조회" */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryTree()));
    }
}
