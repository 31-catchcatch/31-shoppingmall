package com.shoppingmall.domain.inquiry.service;

import com.shoppingmall.domain.inquiry.dto.request.InquiryCreateRequest;
import com.shoppingmall.domain.inquiry.dto.response.InquiryResponse;
import com.shoppingmall.domain.inquiry.entity.Inquiry;
import com.shoppingmall.domain.inquiry.repository.InquiryRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    /** POST /api/v1/customer-center/inquiries - 1:1 문의 접수 */
    @Transactional
    public void createInquiry(Long userId, InquiryCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        inquiryRepository.save(Inquiry.builder()
                .user(user)
                .category(request.getCategory())
                .orderNumber(request.getOrderNumber())
                .title(request.getTitle())
                .content(request.getContent())
                .build());
    }

    /** GET /api/v1/customer-center/inquiries - 내 문의 내역 조회 */
    public Page<InquiryResponse> getMyInquiries(Long userId, Pageable pageable) {
        return inquiryRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(InquiryResponse::from);
    }
}
