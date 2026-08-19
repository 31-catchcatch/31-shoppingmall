package com.shoppingmall.domain.product.controller;

import com.shoppingmall.domain.product.dto.response.BrandResponse;
import com.shoppingmall.domain.product.service.BrandService;
import com.shoppingmall.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 명세서 "공통 - 브랜드 목록 조회" 대응.
 * 목록 조회는 비로그인 허용(SecurityConfig permitAll).
 */
@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getBrands() {
        return ResponseEntity.ok(ApiResponse.success(brandService.getBrands()));
    }
}
