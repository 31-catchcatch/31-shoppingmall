package com.shoppingmall.domain.admin.controller;

import com.shoppingmall.domain.admin.dto.request.AdminInquiryAnswerRequest;
import com.shoppingmall.domain.admin.dto.response.AdminInquiryResponse;
import com.shoppingmall.domain.admin.service.AdminInquiryService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 관리자 - 1:1 고객문의 조회/답변 (customer_inquiries). ADMIN 전용 (SecurityConfig /admin/** 규칙) */
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminInquiryResponse>>> getAllInquiries(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(adminInquiryService.getAllInquiries(pageable)));
    }

    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<ApiResponse<Void>> answerInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody AdminInquiryAnswerRequest request) {
        adminInquiryService.answerInquiry(inquiryId, request.content());
        return ResponseEntity.ok(ApiResponse.success("답변이 등록되었습니다.", null));
    }

    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<Void>> deleteInquiry(@PathVariable Long inquiryId) {
        adminInquiryService.deleteInquiry(inquiryId);
        return ResponseEntity.ok(ApiResponse.success("문의가 삭제되었습니다.", null));
    }
}
