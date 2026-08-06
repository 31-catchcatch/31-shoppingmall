package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.seller.dto.request.SellerSignupRequest;
import com.shoppingmall.domain.seller.dto.response.SellerSignupResponse;
import com.shoppingmall.domain.seller.dto.request.SellerLoginRequest;
import com.shoppingmall.domain.seller.dto.response.SellerLoginResponse;
import com.shoppingmall.domain.seller.entity.Seller;
import com.shoppingmall.domain.seller.entity.SellerApplication;
import com.shoppingmall.domain.seller.entity.SellerApplicationStatus;
import com.shoppingmall.domain.seller.entity.SellerStatus;
import com.shoppingmall.domain.seller.repository.SellerApplicationRepository;
import com.shoppingmall.domain.seller.repository.SellerRepository;
import com.shoppingmall.domain.user.entity.Role;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 판매자 로그인과 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerAuthService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;

    /**
     * 판매자 회원가입 시 함께 생성되는 입점 신청서 Repository
     */
    private final SellerApplicationRepository sellerApplicationRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 판매자 회원가입 (S-AUTH-003)
     *
     * 처리 순서
     * 1. 아이디/이메일 중복 확인
     * 2. 사업자등록번호 중복 확인 (진행 중 또는 승인된 신청 기준)
     * 3. users 에 role=SELLER 로 즉시 저장
     * 4. seller_applications 에 PENDING 으로 즉시 저장
     * 5. 응답 DTO 반환 (관리자 승인 전까지는 sellers 테이블에 행이 없어 로그인 불가)
     */
    @Transactional
    public SellerSignupResponse signup(SellerSignupRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new CustomException(ErrorCode.DUPLICATE_USERNAME);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        String businessRegistrationNumber =
                normalizeDigits(request.businessRegistrationNumber());

        List<SellerApplicationStatus> activeStatuses = List.of(
                SellerApplicationStatus.PENDING,
                SellerApplicationStatus.APPROVED
        );

        boolean businessNumberInUse =
                sellerApplicationRepository
                        .existsByBusinessRegistrationNumberAndStatusIn(
                                businessRegistrationNumber,
                                activeStatuses
                        );

        if (businessNumberInUse) {
            throw new CustomException(ErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .role(Role.SELLER)
                .build();

        User savedUser = userRepository.save(user);

        SellerApplication application = SellerApplication.builder()
                .user(savedUser)
                .businessName(request.businessName())
                .businessRegistrationNumber(businessRegistrationNumber)
                .representativeName(request.representativeName())
                .contactNumber(normalizeDigits(request.contactNumber()))
                .businessAddress(request.businessAddress())
                .businessRegistrationFileUrl(request.businessRegistrationFileUrl())
                .mailOrderReportFileUrl(request.mailOrderReportFileUrl())
                .status(SellerApplicationStatus.PENDING)
                .build();

        SellerApplication savedApplication =
                sellerApplicationRepository.save(application);

        return new SellerSignupResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedApplication.getId(),
                savedApplication.getStatus().name()
        );
    }

    /**
     * 숫자만 남기도록 변환 (사업자등록번호/연락처 공통)
     * 예) 010-1234-5678 → 01012345678
     */
    private String normalizeDigits(String value) {
        return value.replaceAll("[^0-9]", "");
    }

    /**
     * 판매자 로그인
     */
    public SellerLoginResponse login(SellerLoginRequest request) {

        User user = userRepository.findByUsernameAndDeletedFalse(request.loginId())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        Seller seller = sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.SELLER_NOT_APPROVED));

        validateSellerStatus(seller);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), "ROLE_SELLER");
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), "ROLE_SELLER");

        return new SellerLoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                seller.getId(),
                seller.getBusinessName()
        );
    }

    private void validateSellerStatus(Seller seller) {

        SellerStatus status = seller.getStatus();

        if (status == SellerStatus.ACTIVE) {
            return;
        }

        if (status == SellerStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.SELLER_SUSPENDED);
        }

        if (status == SellerStatus.CLOSED || status == SellerStatus.FORCED_CLOSED) {
            throw new CustomException(ErrorCode.SELLER_NOT_APPROVED);
        }

        throw new CustomException(ErrorCode.INVALID_SELLER_STATUS);
    }
}