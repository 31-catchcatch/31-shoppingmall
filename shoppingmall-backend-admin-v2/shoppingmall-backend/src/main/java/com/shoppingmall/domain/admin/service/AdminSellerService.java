package com.shoppingmall.domain.admin.service;

import com.shoppingmall.domain.admin.dto.request.ReviewDecisionRequest;
import com.shoppingmall.domain.admin.dto.request.SellerStatusUpdateRequest;
import com.shoppingmall.domain.admin.dto.response.AdminSellerResponse;
import com.shoppingmall.domain.seller.dto.response.SellerApplicationResponse;
import com.shoppingmall.domain.seller.entity.Seller;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.entity.SellerStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.domain.seller.repository.SellerRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * API 명세서 "관리자 - 운영" 중 판매자/입점 관련 API 담당.
 * - GET  /admin/sellers                                (승인된 입점업체 전체 목록)
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

    /**
     * 승인된 입점업체 전체 목록.
     * 프론트(admin-sellers.js)가 검색어/상태 필터를 전부 클라이언트 쪽에서
     * 처리하므로 서버는 항상 전체 목록을 내려준다.
     */
    public List<AdminSellerResponse> getSellers() {
        return sellerRepository.findAll().stream()
                .map(AdminSellerResponse::from)
                .toList();
    }

    /** 대기 중인(PENDING) 입점 신청서 목록. 필요하면 다른 상태도 조회할 수 있게 status로 필터링. */
    public List<SellerApplicationResponse> getApplications(SellerApplicationStatus status) {
        SellerApplicationStatus target = status == null ? SellerApplicationStatus.PENDING : status;
        return sellerApplicationRepository.findAllByStatusOrderByCreatedAtAsc(target).stream()
                .map(SellerApplicationResponse::from)
                .toList();
    }

    /**
     * 입점 신청 승인/반려.
     * 승인 시: 신청서 상태 변경 + 실제 Seller 계정 생성.
     * (User.role 은 이제 SellerAuthService.signup() 가입 시점에 이미 SELLER로 저장되므로
     *  여기서 별도로 승격시키지 않는다.)
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

            Seller seller = Seller.builder()
                    .user(application.getUser())
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