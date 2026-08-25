package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.auth.service.AuthService;
import com.shoppingmall.domain.seller.dto.request.SellerProfileUpdateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerProfileResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET/PUT /api/v1/seller/me - 판매자 마이페이지 회원정보 조회/수정.
 * 프론트(seller-mypage-edit.js)가 이 경로를 호출한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProfileService {

    private final SellerApplicationRepository sellerApplicationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;   // [4-2 조치] 비밀번호 변경 시 세션 종료

    public SellerProfileResponse getMyProfile(Long userId) {
        return SellerProfileResponse.from(findApprovedApplication(userId));
    }

    @Transactional
    public void updateMyProfile(Long userId, SellerProfileUpdateRequest request) {
        SellerApplication application = findApprovedApplication(userId);
        User user = application.getUser();

        // 비밀번호 변경 (선택): 둘 다 온 경우에만, 현재 비밀번호 검증 후 변경
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_INPUT); // "현재 비밀번호가 일치하지 않습니다."
            }
            user.changePassword(passwordEncoder.encode(request.getNewPassword()));

            // [4-2 조치] 일반 사용자와 같은 규칙. 기존 세션을 전부 끊는다.
            authService.terminateAllSessions(userId);
        }

        user.changeEmail(request.getEmail());
        application.updateProfile(
                request.getBusinessName(),
                request.getRepresentativeName(),
                request.getManagerPhone(),
                request.getBusinessAddress()
        );
    }

    private SellerApplication findApprovedApplication(Long userId) {
        SellerApplication application = sellerApplicationRepository
                .findFirstByUser_IdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCESS_DENIED));
        if (application.getStatus() != SellerApplicationStatus.APPROVED) {
            throw new CustomException(ErrorCode.ACCESS_DENIED); // 승인된 판매자만 마이페이지 접근 가능
        }
        return application;
    }
}
