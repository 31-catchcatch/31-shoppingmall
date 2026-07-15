package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.seller.dto.request.SellerApplicationCreateRequest;
import com.shoppingmall.domain.seller.dto.response.SellerApplicationResponse;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 판매자 입점 신청 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerApplicationService {

    // 입점 신청 Repository
    private final SellerApplicationRepository sellerApplicationRepository;

    // 회원 조회 Repository
    private final UserRepository userRepository;

    /**
     * 판매자 입점 신청
     *
     * 처리 순서
     * 1. 회원 존재 여부 확인
     * 2. 이미 진행 중인 입점 신청이 있는지 확인
     * 3. 사업자등록번호 중복 확인
     * 4. 신청서 생성
     * 5. DB 저장
     * 6. 응답 DTO 반환
     */
    @Transactional
    public SellerApplicationResponse createApplication(
            Long userId,
            SellerApplicationCreateRequest request
    ) {

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        // 2. 이미 신청한 이력이 있는지 확인
        validateDuplicateApplication(userId);

        // 3. 사업자등록번호 형식 통일(하이픈 제거)
        String businessRegistrationNumber =
                normalizeBusinessRegistrationNumber(
                        request.businessRegistrationNumber()
                );

        // 4. 사업자등록번호 중복 확인
        validateDuplicateBusinessNumber(
                businessRegistrationNumber
        );

        // 5. Entity 생성
        SellerApplication application = SellerApplication.builder()
                .user(user)
                .businessName(request.businessName())
                .businessRegistrationNumber(
                        businessRegistrationNumber
                )
                .representativeName(
                        request.representativeName()
                )
                .contactNumber(
                        normalizeContactNumber(
                                request.contactNumber()
                        )
                )
                .businessAddress(
                        request.businessAddress()
                )
                .businessRegistrationFileUrl(
                        request.businessRegistrationFileUrl()
                )
                .mailOrderReportFileUrl(
                        request.mailOrderReportFileUrl()
                )
                .status(SellerApplicationStatus.PENDING)
                .build();

        // 6. DB 저장
        SellerApplication saved =
                sellerApplicationRepository.save(application);

        // 7. Response DTO 반환
        return SellerApplicationResponse.from(saved);
    }

    /**
     * 동일 사용자가 이미 신청한 내역이 있는지 확인
     */
    private void validateDuplicateApplication(Long userId) {

        List<SellerApplicationStatus> activeStatuses = List.of(
                SellerApplicationStatus.PENDING,
                SellerApplicationStatus.APPROVED
        );

        boolean exists =
                sellerApplicationRepository
                        .existsByUser_IdAndStatusIn(
                                userId,
                                activeStatuses
                        );

        if (exists) {
            throw new CustomException(
                    ErrorCode.SELLER_APPLICATION_ALREADY_EXISTS
            );
        }
    }

    /**
     * 사업자등록번호 중복 확인
     */
    private void validateDuplicateBusinessNumber(
            String businessRegistrationNumber
    ) {

        List<SellerApplicationStatus> activeStatuses = List.of(
                SellerApplicationStatus.PENDING,
                SellerApplicationStatus.APPROVED
        );

        boolean exists =
                sellerApplicationRepository
                        .existsByBusinessRegistrationNumberAndStatusIn(
                                businessRegistrationNumber,
                                activeStatuses
                        );

        if (exists) {
            throw new CustomException(
                    ErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS
            );
        }
    }

    /**
     * 사업자등록번호를 숫자만 남기도록 변환
     *
     * 예)
     * 123-45-67890
     * ↓
     * 1234567890
     */
    private String normalizeBusinessRegistrationNumber(String value) {
        return value.replaceAll("[^0-9]", "");
    }

    /**
     * 연락처를 숫자만 남기도록 변환
     *
     * 예)
     * 010-1234-5678
     * ↓
     * 01012345678
     */
    private String normalizeContactNumber(String value) {
        return value.replaceAll("[^0-9]", "");
    }
}