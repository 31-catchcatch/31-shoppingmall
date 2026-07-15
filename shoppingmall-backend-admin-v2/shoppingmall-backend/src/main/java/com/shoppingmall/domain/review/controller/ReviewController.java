package com.shoppingmall.domain.review.controller;

import com.shoppingmall.domain.review.dto.request.ReviewCreateRequest;
import com.shoppingmall.domain.review.dto.response.ReviewResponse;
import com.shoppingmall.domain.review.service.ReviewService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 1. 배송이 끝난 상품에 대한 신규 리뷰 작성 (POST) - 토큰 인증 필수
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<Void>> createReview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReviewCreateRequest request) {

        reviewService.createReview(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("리뷰 작성이 안전하게 등록 완료되었습니다.", null));
    }

    // 2. 특정 상품 상세 페이지 하단 리뷰 목록 조회 (GET) - 비로그인 오픈 대상
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 5) Pageable pageable) { // 기본 한 페이지에 5개씩 리뷰 노출

        Page<ReviewResponse> response = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success("상품 리뷰 목록 조회가 완료되었습니다.", PageResponse.from(response)));
    }
}