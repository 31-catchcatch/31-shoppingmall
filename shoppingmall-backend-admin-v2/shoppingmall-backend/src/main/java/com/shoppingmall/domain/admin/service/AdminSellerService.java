package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.request.ReviewDecisionRequest;
import com.shoppingmall.domain.admin.dto.request.SellerStatusUpdateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerApplicationResponse;
import com.shoppingmall.domain.seller.entity.Seller;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.entity.SellerStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.domain.seller.repository.SellerRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * API 명세서 "관리자 - 운영" 중 판매자/입점 관련 4개 API 담당.
 * - GET  /admin/sellers/applications
 * - POST /admin/sellers/applications/{appId}/status  (승인/반려)
 * - PUT  /admin/sellers/{sellerId}/status            (정상/정지/폐점)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSellerService {

    private final SellerApplicationRepository sellerApplicationRepository;
    private final SellerRepository sellerRepository;

    /** 대기 중인(PENDING) 입점 신청서 목록. 필요하면 다른 상태도 조회할 수 있게 status로 필터링. */
    public List<SellerApplicationResponse> getApplications(SellerApplicationStatus status) {
        SellerApplicationStatus target = status == null ? SellerApplicationStatus.PENDING : status;
        return sellerApplicationRepository.findAllByStatusOrderByCreatedAtAsc(target).stream()
                .map(SellerApplicationResponse::from)
                .toList();
    }

    /**
     * 입점 신청 승인/반려.
     * 승인 시: 신청서 상태 변경 + 실제 Seller 계정 생성 + 신청자 User.role을 SELLER로 승격.
     * (이 연결 로직이 이전까지 빠져있던 부분 - 관리자 도메인 작업하면서 채워 넣음)
     */
    @Transactional
    public SellerApplicationResponse reviewApplication(Long appId, ReviewDecisionRequest request) {
        SellerApplication application = sellerApplicationRepository.findById(appId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLER_APPLICATION_NOT_FOUND));

        if (application.getStatus() != SellerApplicationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_SELLER_STATUS);
        }

        if (request.decision() == ReviewDecisionRequest.Decision.APPROVE) {
            application.approve();

            User applicant = application.getUser();
            applicant.promoteToSeller();

            Seller seller = Seller.builder()
                    .user(applicant)
                    .businessName(application.getBusinessName())
                    .businessRegistrationNumber(application.getBusinessRegistrationNumber())
                    .representativeName(application.getRepresentativeName())
                    .contactNumber(application.getContactNumber())
                    .businessAddress(application.getBusinessAddress())
                    .build(); // status 기본값 ACTIVE (Seller.@Builder.Default)

            sellerRepository.save(seller);
        } else {
            application.reject(request.rejectionReason());
        }

        return SellerApplicationResponse.from(application);
    }

    /** 승인된 판매자의 운영 상태 변경 (정상/일시정지/자진폐점/강제폐점) */
    @Transactional
    public void updateSellerStatus(Long sellerId, SellerStatusUpdateRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new CustomException(ErrorCode.SELLER_NOT_FOUND));

        SellerStatus status = request.status();
        switch (status) {
            case ACTIVE -> seller.activate();
            case SUSPENDED -> seller.suspend();
            case CLOSED -> seller.close();
            case FORCED_CLOSED -> seller.forceClose();
            default -> throw new CustomException(ErrorCode.INVALID_SELLER_STATUS);
        }
    }
}
