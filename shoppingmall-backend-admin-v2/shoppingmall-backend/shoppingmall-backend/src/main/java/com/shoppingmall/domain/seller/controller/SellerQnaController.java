package com.shoppingmall.domain.seller.controller;

import com.shoppingmall.domain.seller.dto.request.SellerQnaAnswerRequest;
import com.shoppingmall.domain.seller.dto.request.SellerQnaSearchRequest;
import com.shoppingmall.domain.seller.dto.response.SellerQnaAnswerResponse;
import com.shoppingmall.domain.seller.dto.response.SellerQnaResponse;
import com.shoppingmall.domain.seller.service.SellerQnaService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seller/qna")
@RequiredArgsConstructor
public class SellerQnaController {

    private final SellerQnaService sellerQnaService;

    // 판매자 상품 Q&A 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SellerQnaResponse>>> getQnaList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute SellerQnaSearchRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        Page<SellerQnaResponse> response =
                sellerQnaService.getQnaList(userId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Q&A 답변 등록 또는 수정
    @PostMapping("/{qnaId}/answers")
    public ResponseEntity<ApiResponse<SellerQnaAnswerResponse>> registerAnswer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long qnaId,
            @Valid @RequestBody SellerQnaAnswerRequest request
    ) {
        Long userId = userDetails.getUser().getId();

        SellerQnaAnswerResponse response =
                sellerQnaService.registerAnswer(userId, qnaId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Q&A 논리 삭제
    @DeleteMapping("/{qnaId}")
    public ResponseEntity<ApiResponse<Void>> deleteQna(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long qnaId
    ) {
        Long userId = userDetails.getUser().getId();

        sellerQnaService.deleteQna(userId, qnaId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
