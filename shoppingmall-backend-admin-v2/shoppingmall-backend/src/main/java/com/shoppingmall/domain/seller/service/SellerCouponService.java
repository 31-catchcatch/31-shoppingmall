package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.coupon.entity.CouponRequest;
import com.shoppingmall.domain.coupon.entity.CouponRequestStatus;
import com.shoppingmall.domain.coupon.repository.CouponRequestRepository;
import com.shoppingmall.domain.seller.dto.request.SellerCouponCreateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerCouponResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.global.common.PageResponse;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 쿠폰 발행 요청 서비스
 *
 * 담당 API
 * POST /api/v1/seller/coupons/request
 * GET  /api/v1/seller/coupons/requests
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerCouponService {

    private final CouponRequestRepository couponRequestRepository;
    private final SellerApplicationRepository sellerApplicationRepository;

    /**
     * 판매자가 관리자에게 쿠폰 발행 승인을 요청한다.
     *
     * 처리 순서
     * 1. 로그인 사용자가 승인된 판매자인지 확인
     * 2. 쿠폰 유효기간 검증
     * 3. 할인 정보 검증
     * 4. 동일한 처리 중 요청이 있는지 확인
     * 5. 쿠폰 요청 Entity 생성
     * 6. DB 저장
     * 7. 응답 DTO 반환
     *
     * @param userId 로그인한 사용자 ID
     * @param request 쿠폰 발행 요청 정보
     * @return 생성된 쿠폰 발행 요청 정보
     */
    @Transactional
    public SellerCouponResponse createCouponRequest(
            Long userId,
            SellerCouponCreateRequest request
    ) {
        // 1. 현재 사용자가 승인된 판매자인지 확인한다.
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        // 2. 쿠폰 시작일과 종료일을 검증한다.
        validateCouponPeriod(request);

        // 3. 할인 유형과 할인 값을 검증한다.
        validateDiscount(request);

        // 4. 동일한 이름으로 처리 중인 쿠폰 요청이 있는지 확인한다.
        boolean alreadyExists =
                couponRequestRepository
                        .existsBySeller_IdAndCouponNameAndStatus(
                                seller.getId(),
                                request.couponName(),
                                CouponRequestStatus.PENDING
                        );

        if (alreadyExists) {
            throw new CustomException(
                    ErrorCode.COUPON_REQUEST_ALREADY_EXISTS
            );
        }

        // 5. 쿠폰 발행 요청 Entity를 생성한다.
        CouponRequest couponRequest = CouponRequest.builder()
                .seller(seller)
                .couponName(request.couponName())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .minimumOrderAmount(
                        request.minimumOrderAmount()
                )
                .maximumDiscountAmount(
                        request.maximumDiscountAmount()
                )
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .totalQuantity(request.totalQuantity())
                .status(CouponRequestStatus.PENDING)
                .build();

        // 6. 쿠폰 발행 요청을 DB에 저장한다.
        CouponRequest savedRequest =
                couponRequestRepository.save(couponRequest);

        // 7. 응답 DTO로 변환해서 반환한다.
        return SellerCouponResponse.from(savedRequest);
    }

    /**
     * 로그인한 판매자 본인의 쿠폰 발행 요청 목록을 조회한다.
     *
     * 다른 판매자의 요청이 섞이지 않도록
     * 승인된 입점 신청 ID로만 조회한다.
     *
     * @param userId 로그인한 사용자 ID
     * @param pageable 페이징 정보
     * @return 본인이 요청한 쿠폰 발행 요청 목록
     */
    public PageResponse<SellerCouponResponse> getMyCouponRequests(
            Long userId,
            Pageable pageable
    ) {
        SellerApplication seller =
                getApprovedSellerApplication(userId);

        Page<SellerCouponResponse> page =
                couponRequestRepository
                        .findAllBySeller_IdOrderByCreatedAtDesc(
                                seller.getId(),
                                pageable
                        )
                        .map(SellerCouponResponse::from);

        return PageResponse.from(page);
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
     * 쿠폰 종료일이 시작일보다 이후인지 확인한다.
     */
    private void validateCouponPeriod(
            SellerCouponCreateRequest request
    ) {
        if (!request.validUntil()
                .isAfter(request.validFrom())) {
            throw new CustomException(
                    ErrorCode.INVALID_COUPON_PERIOD
            );
        }
    }

    /**
     * 쿠폰 할인 유형과 할인 값을 검증한다.
     *
     * PERCENTAGE:
     * 할인율은 1~100 사이여야 한다.
     *
     * FIXED_AMOUNT:
     * 할인 금액은 0보다 커야 한다.
     */
    private void validateDiscount(
            SellerCouponCreateRequest request
    ) {
        String discountType =
                request.discountType().trim().toUpperCase();

        if ("PERCENTAGE".equals(discountType)) {
            if (request.discountValue().compareTo(
                    java.math.BigDecimal.ONE
            ) < 0
                    || request.discountValue().compareTo(
                    java.math.BigDecimal.valueOf(100)
            ) > 0) {
                throw new CustomException(
                        ErrorCode.INVALID_COUPON_DISCOUNT
                );
            }

            return;
        }

        if ("FIXED_AMOUNT".equals(discountType)) {
            if (request.discountValue().signum() <= 0) {
                throw new CustomException(
                        ErrorCode.INVALID_COUPON_DISCOUNT
                );
            }

            return;
        }

        throw new CustomException(
                ErrorCode.INVALID_COUPON_DISCOUNT
        );
    }
}
