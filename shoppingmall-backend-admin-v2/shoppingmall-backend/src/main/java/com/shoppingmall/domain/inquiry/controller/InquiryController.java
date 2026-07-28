package com.shoppingmall.domain.inquiry.controller;

import com.shoppingmall.domain.inquiry.dto.request.InquiryCreateRequest;
import com.shoppingmall.domain.inquiry.dto.response.InquiryResponse;
import com.shoppingmall.domain.inquiry.service.InquiryService;
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

/** 고객센터 1:1 문의 (프론트 customercenter.js 용, 로그인 필요) */
@RestController
@RequestMapping("/api/v1/customer-center/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createInquiry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InquiryCreateRequest request) {
        inquiryService.createInquiry(userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("문의가 접수되었습니다. 순차적으로 답변드리겠습니다.", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InquiryResponse>>> getMyInquiries(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<InquiryResponse> response =
                inquiryService.getMyInquiries(userDetails.getUser().getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(response)));
    }

    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<Void>> deleteInquiry(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long inquiryId) {
        inquiryService.deleteMyInquiry(userDetails.getUser().getId(), inquiryId);
        return ResponseEntity.ok(ApiResponse.success("문의가 삭제되었습니다.", null));
    }
}
