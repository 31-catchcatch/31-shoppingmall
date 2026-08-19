package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.service.AdminQnaService;
import com.shoppingmall.domain.qna.dto.response.QnaResponse;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API 명세서 "관리자 - 운영 - 전체 Q&A 모니터링 조회" */
@RestController
@RequestMapping("/api/v1/admin/qna")
@RequiredArgsConstructor
public class AdminQnaController {

    private final AdminQnaService adminQnaService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<QnaResponse>>> getAllQna(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminQnaService.getAllQna(pageable)));
    }
}
