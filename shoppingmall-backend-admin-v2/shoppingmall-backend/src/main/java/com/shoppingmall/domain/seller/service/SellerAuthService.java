package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.auth.dto.response.TokenResponse;
import com.shoppingmall.domain.auth.service.AuthService;
import com.shoppingmall.domain.seller.dto.request.SellerSignupRequest;
import com.shoppingmall.domain.seller.dto.response.SellerSignupResponse;
import com.shoppingmall.domain.seller.dto.request.SellerLoginRequest;
import com.shoppingmall.domain.seller.dto.response.SellerLoginResponse;
import com.shoppingmall.domain.seller.dto.response.SellerLoginResult;
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
import com.shoppingmall.global.security.LoginAttemptService;
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
    private final LoginAttemptService loginAttemptService;   // [3-2 조치]

    /**
     * [4-2 조치] 토큰 발급을 AuthService 로 위임하기 위해 주입한다.
     * 일반 사용자·관리자와 같은 경로(issueAndPersistTokens)를 쓰기 위함이며,
     * AuthService 는 SellerAuthService 를 참조하지 않아 순환 의존이 없다.
     */
    private final AuthService authService;

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
     *
     * <p><b>[4-2 조치] @Transactional 필수</b> — 이 클래스는 클래스 레벨이
     * {@code @Transactional(readOnly = true)} 라, 이 메서드에서 호출하는
     * {@code issueAndPersistTokens()} 의 refresh_tokens INSERT 가 read-only 커넥션에서
     * 실행되어 실패한다. 로그인 실패 횟수 기록(LoginAttemptService)은 REQUIRES_NEW 라
     * 영향이 없지만, 토큰 저장은 이 트랜잭션에 참여하므로 반드시 read-write 여야 한다.
     */
    @Transactional
    public SellerLoginResult login(SellerLoginRequest request) {

        User user = userRepository.findByUsernameAndDeletedFalse(request.loginId())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        // [3-2 조치] 잠금 확인 -> 비밀번호 대조 -> 결과 기록
        loginAttemptService.assertNotLocked(user);

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPassword()
        );

        if (!passwordMatches) {
            loginAttemptService.onFailure(user.getId());
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        Seller seller = sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.SELLER_NOT_APPROVED));

        validateSellerStatus(seller);

        loginAttemptService.onSuccess(user.getId());   // [3-2 조치]

        // [4-2 조치] 토큰 발급을 일반 사용자·관리자와 같은 경로로 통일한다.
        //
        // 종전에는 jwtTokenProvider 를 직접 호출해 토큰만 만들고 refresh_tokens 에 저장하지
        // 않았다. 그래서 AuthService.refresh() 의 findByToken() 이 항상 실패해
        // 판매자만 토큰 재발급이 불가능했고, 로그아웃 시에도 지울 행이 없어
        // 리프레시 토큰이 만료(7일)까지 살아남았다.
        //
        // role 도 "ROLE_SELLER" 직접 지정 대신 Role.name() 을 쓰는 다른 경로와 맞춘다.
        // (인가는 CustomUserDetails 가 DB 의 User.role 로 판단하므로 동작 변화는 없다)
        TokenResponse tokens = authService.issueAndPersistTokens(user);

        return new SellerLoginResult(
                tokens,
                seller.getId(),
                seller.getBusinessName(),
                tokens.role()
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