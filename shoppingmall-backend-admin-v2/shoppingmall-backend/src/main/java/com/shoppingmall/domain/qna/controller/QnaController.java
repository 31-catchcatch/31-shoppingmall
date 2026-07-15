package com.shoppingmall.domain.qna.controller;

import com.shoppingmall.domain.qna.dto.request.QnaCreateRequest;
import com.shoppingmall.domain.qna.dto.response.QnaListResponse;
import com.shoppingmall.domain.qna.dto.response.QnaResponse;
import com.shoppingmall.domain.qna.service.QnaService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** API 명세서 "일반 사용자 - 소통 - 상품 문의" */
@RestController
@RequestMapping("/api/v1/products/{productId}/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    /** GET /products/{productId}/qna - 비로그인도 조회 가능 (비밀글은 서비스단에서 마스킹) */
    @GetMapping
    public ResponseEntity<ApiResponse<QnaListResponse>> getProductQnaList(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long currentUserId = (userDetails != null) ? userDetails.getUser().getId() : null;
        QnaListResponse response = qnaService.getProductQnaList(productId, currentUserId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** POST /products/{productId}/qna - 로그인 필요 */
    @PostMapping
    public ResponseEntity<ApiResponse<QnaResponse>> createQna(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody QnaCreateRequest request) {

        QnaResponse response = qnaService.createQna(userDetails.getUser().getId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
