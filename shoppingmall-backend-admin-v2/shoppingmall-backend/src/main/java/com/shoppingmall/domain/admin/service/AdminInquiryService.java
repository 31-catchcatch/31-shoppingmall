package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.response.AdminInquiryResponse;
import com.shoppingmall.domain.inquiry.entity.Inquiry;
import com.shoppingmall.domain.inquiry.repository.InquiryRepository;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 - 1:1 고객문의 조회/답변.
 * 상품 QnA(AdminQnaService)와 달리 customer_inquiries 테이블을 다룬다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;

    /** GET /api/v1/admin/inquiries - 전체 1:1 문의 목록 (최신순) */
    public PageResponse<AdminInquiryResponse> getAllInquiries(Pageable pageable) {
        Page<AdminInquiryResponse> page =
                inquiryRepository.findAll(pageable).map(AdminInquiryResponse::from);
        return PageResponse.from(page);
    }

    /** POST /api/v1/admin/inquiries/{id}/answer - 관리자 답변 등록 (상태 ANSWERED 전환) */
    @Transactional
    public void answerInquiry(Long inquiryId, String content) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
        inquiry.answer(content); // 엔티티 메서드: answer 세팅 + status ANSWERED + answeredAt
    }

    /** DELETE /api/v1/admin/inquiries/{id} - 관리자 문의 삭제 (소유권 무관, ADMIN 권한) */
    @Transactional
    public void deleteInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
        inquiryRepository.delete(inquiry);
    }
}
