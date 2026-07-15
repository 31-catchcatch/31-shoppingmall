package com.shoppingmall.domain.banner.controller;

import com.shoppingmall.domain.banner.dto.response.BannerResponse;
import com.shoppingmall.domain.banner.service.BannerService;
import com.shoppingmall.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API 명세서 "일반 사용자 - 상품 - 이벤트 배너 조회" 대응.
 * SecurityConfig 에 이미 permitAll 로 등록되어 있음 (비로그인 접근 가능).
 */
@RestController
@RequestMapping("/api/v1/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getBanners() {
        List<BannerResponse> response = bannerService.getActiveBanners();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
