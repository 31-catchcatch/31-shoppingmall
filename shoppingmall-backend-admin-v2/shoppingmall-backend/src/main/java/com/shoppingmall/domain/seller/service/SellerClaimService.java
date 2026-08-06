package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.claim.entity.Claim;
import com.shoppingmall.domain.claim.entity.ClaimStatus;
import com.shoppingmall.domain.claim.entity.ClaimType;
import com.shoppingmall.domain.claim.repository.ClaimRepository;
import com.shoppingmall.domain.seller.dto.request.SellerClaimSearchRequest;
import com.shoppingmall.domain.seller.dto.request.SellerClaimStatusUpdateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerClaimResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 교환/환불 클레임 관리 서비스
 *
 * 담당 API
 * GET /api/v1/seller/claims
 * PUT /api/v1/seller/claims/{claimId}/status
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerClaimService {

    private final ClaimRepository claimRepository;
    private final SellerApplicationRepository sellerApplicationRepository;

    /**
     * 판매자 상품에 접수된 클레임 목록 조회
     *
     * 처리 순서
     * 1. 승인된 판매자 확인
     * 2. 검색 조건 변환
     * 3. 페이지 정보 보정
     * 4. 판매자 상품에 해당하는 클레임 조회
     * 5. 응답 DTO로 변환
     */
    public Page<SellerClaimResponse> getClaims(
            Long userId,
            SellerClaimSearchRequest request
    ) {
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        int page = request.page() == null
                ? 0
                : Math.max(request.page(), 0);

        int size = request.size() == null
                ? 20
                : Math.min(Math.max(request.size(), 1), 100);

        Pageable pageable = PageRequest.of(page, size);

        ClaimType claimType =
                parseClaimType(request.claimType());

        ClaimStatus claimStatus =
                parseClaimStatus(request.status());

        Page<Claim> claimPage;

        /*
         * 클레임 유형과 상태가 모두 전달된 경우
         */
        if (claimType != null && claimStatus != null) {
            claimPage = claimRepository
                    .findAllByOrderDetail_Product_Seller_IdAndTypeAndStatusOrderByCreatedAtDesc(
                            seller.getId(),
                            claimType,
                            claimStatus,
                            pageable
                    );

            /*
             * 클레임 유형만 전달된 경우
             */
        } else if (claimType != null) {
            claimPage = claimRepository
                    .findAllByOrderDetail_Product_Seller_IdAndTypeOrderByCreatedAtDesc(
                            seller.getId(),
                            claimType,
                            pageable
                    );

            /*
             * 클레임 상태만 전달된 경우
             */
        } else if (claimStatus != null) {
            claimPage = claimRepository
                    .findAllByOrderDetail_Product_Seller_IdAndStatusOrderByCreatedAtDesc(
                            seller.getId(),
                            claimStatus,
                            pageable
                    );

            /*
             * 검색 조건이 없는 경우 판매자의 전체 클레임 조회
             */
        } else {
            claimPage = claimRepository
                    .findAllByOrderDetail_Product_Seller_IdOrderByCreatedAtDesc(
                            seller.getId(),
                            pageable
                    );
        }

        return claimPage.map(SellerClaimResponse::from);
    }

    /**
     * 판매자가 클레임 상태를 변경한다.
     *
     * 처리 순서
     * 1. 승인된 판매자 확인
     * 2. 클레임 단건 조회
     * 3. 판매자 소유 상품인지 확인
     * 4. 요청 상태 문자열을 Enum으로 변환
     * 5. 현재 상태에서 변경 가능한지 확인
     * 6. 접수 또는 반려 처리
     * 7. 응답 DTO 반환
     */
    @Transactional
    public SellerClaimResponse updateClaimStatus(
            Long userId,
            Long claimId,
            SellerClaimStatusUpdateRequest request
    ) {
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() ->
                        new CustomException(
                                ErrorCode.CLAIM_NOT_FOUND
                        )
                );

        validateClaimOwnership(claim, seller);

        ClaimStatus targetStatus =
                parseRequiredClaimStatus(request.status());

        validateStatusChange(
                claim.getStatus(),
                targetStatus,
                claim.getType()
        );

        /*
         * 상태별 도메인 메서드 호출
         *
         * 상태를 외부에서 직접 set하지 않고
         * Entity 메서드를 통해 변경한다.
         */
        switch (targetStatus) {
            case ACCEPTED ->
                    claim.accept(request.reason());

            case REJECTED ->
                    claim.reject(request.reason());

            case PROCESSING ->
                    claim.startProcessing(request.reason());

            // 교환(EXCHANGE) 건은 결제 취소가 얽혀 있지 않아 이 API에서 바로 완료 처리한다.
            // 환불(RETURN) 건의 완료 처리는 POST /seller/refunds(SellerRefundService)에서만 허용한다.
            case COMPLETED ->
                    claim.complete();

            default ->
                    throw new CustomException(
                            ErrorCode.INVALID_CLAIM_STATUS
                    );
        }

        return SellerClaimResponse.from(claim);
    }

    /**
     * 가장 최근 입점 신청이 승인 상태인지 확인한다.
     */
    private SellerApplication getApprovedSellerApplication(
            Long userId
    ) {
        SellerApplication application =
                sellerApplicationRepository
                        .findFirstByUser_IdOrderByCreatedAtDesc(userId)
                        .orElseThrow(() ->
                                new CustomException(
                                        ErrorCode.SELLER_NOT_APPROVED
                                )
                        );

        if (application.getStatus()
                != SellerApplicationStatus.APPROVED) {
            throw new CustomException(
                    ErrorCode.SELLER_NOT_APPROVED
            );
        }

        return application;
    }

    /**
     * 해당 클레임이 현재 판매자의 상품에 대한 요청인지 확인한다.
     */
    private void validateClaimOwnership(
            Claim claim,
            SellerApplication seller
    ) {
        Long claimSellerId = claim
                .getOrderDetail()
                .getProduct()
                .getSeller()
                .getId();

        if (!claimSellerId.equals(seller.getId())) {
            throw new CustomException(
                    ErrorCode.ACCESS_DENIED
            );
        }
    }

    /**
     * 클레임 유형 문자열을 Enum으로 변환한다.
     *
     * 검색 조건이 없으면 null을 반환한다.
     */
    private ClaimType parseClaimType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return ClaimType.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT
            );
        }
    }

    /**
     * 검색용 클레임 상태 문자열을 Enum으로 변환한다.
     */
    private ClaimStatus parseClaimStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return ClaimStatus.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException e) {
            throw new CustomException(
                    ErrorCode.INVALID_CLAIM_STATUS
            );
        }
    }

    /**
     * 상태 변경 요청은 값이 반드시 필요하다.
     */
    private ClaimStatus parseRequiredClaimStatus(
            String value
    ) {
        if (value == null || value.isBlank()) {
            throw new CustomException(
                    ErrorCode.INVALID_CLAIM_STATUS
            );
        }

        return parseClaimStatus(value);
    }

    /**
     * 허용된 클레임 상태 변경인지 확인한다.
     *
     * 예시 흐름:
     * REQUESTED → ACCEPTED
     * REQUESTED → REJECTED
     * ACCEPTED  → PROCESSING
     * PROCESSING → COMPLETED (교환(EXCHANGE) 건만 허용. 환불(RETURN) 건은
     *                          결제 취소가 얽혀 있어 SellerRefundService에서만 완료 처리한다.)
     */
    private void validateStatusChange(
            ClaimStatus currentStatus,
            ClaimStatus targetStatus,
            ClaimType claimType
    ) {
        boolean valid =
                (currentStatus == ClaimStatus.REQUESTED
                        && targetStatus == ClaimStatus.ACCEPTED)
                        ||
                        (currentStatus == ClaimStatus.REQUESTED
                                && targetStatus == ClaimStatus.REJECTED)
                        ||
                        (currentStatus == ClaimStatus.ACCEPTED
                                && targetStatus == ClaimStatus.PROCESSING)
                        ||
                        (currentStatus == ClaimStatus.PROCESSING
                                && targetStatus == ClaimStatus.COMPLETED
                                && claimType == ClaimType.EXCHANGE);

        if (!valid) {
            throw new CustomException(
                    ErrorCode.INVALID_CLAIM_STATUS
            );
        }
    }
}