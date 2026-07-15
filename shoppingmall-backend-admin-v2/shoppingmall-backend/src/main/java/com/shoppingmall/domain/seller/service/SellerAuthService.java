package com.shoppingmall.domain.seller.service;

import com.shoppingmall.domain.seller.dto.request.SellerLoginRequest;
import com.shoppingmall.domain.seller.dto.response.SellerLoginResponse;
import com.shoppingmall.domain.seller.entity.Seller;
import com.shoppingmall.domain.seller.entity.SellerStatus;
import com.shoppingmall.domain.seller.repository.SellerRepository;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 로그인과 인증 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerAuthService {

    /**
     * 로그인 아이디로 사용자 계정을 조회하기 위한 Repository
     */
    private final UserRepository userRepository;

    /**
     * 일반 사용자와 연결된 판매자 정보를 조회하기 위한 Repository
     */
    private final SellerRepository sellerRepository;

    /**
     * 평문 비밀번호와 암호화된 비밀번호를 비교한다.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 로그인에 성공한 판매자의 JWT를 생성한다.
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 판매자 로그인
     *
     * 처리 순서
     * 1. 로그인 아이디로 사용자 조회
     * 2. 비밀번호 일치 여부 확인
     * 3. 사용자와 연결된 판매자 조회
     * 4. 판매자 계정 상태 확인
     * 5. JWT Access Token 및 Refresh Token 생성
     * 6. 로그인 응답 DTO 반환
     *
     * @param request 판매자 로그인 정보
     * @return 로그인 토큰과 판매자 정보
     */
    public SellerLoginResponse login(SellerLoginRequest request) {

        // 1. 입력된 로그인 아이디로 사용자 계정을 조회한다.
        User user = userRepository.findByUsernameAndDeletedFalse(request.loginId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.LOGIN_FAILED)
                );

        // 2. 입력된 비밀번호와 DB에 저장된 암호화 비밀번호를 비교한다.
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPassword()
        );

        // 아이디 존재 여부와 비밀번호 오류를 구분해서 응답하면
        // 계정 존재 여부가 노출될 수 있으므로 동일한 로그인 실패 오류를 사용한다.
        if (!passwordMatches) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        // 3. 일반 사용자 계정과 연결된 판매자 정보를 조회한다.
        Seller seller = sellerRepository.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new CustomException(ErrorCode.SELLER_NOT_APPROVED)
                );

        // 4. 판매자 계정의 현재 상태를 검사한다.
        validateSellerStatus(seller);

        /*
         * 5. JWT 생성
         *
         * JwtTokenProvider의 실제 메서드 이름은 프로젝트 구현에 따라
         * createAccessToken, generateAccessToken 등으로 다를 수 있다.
         */
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                "ROLE_SELLER"
        );

        String refreshToken = jwtTokenProvider.createRefreshToken(
                user.getId(),
                "ROLE_SELLER"
        );

        // 6. 로그인 결과를 응답 DTO로 반환한다.
        return new SellerLoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                seller.getId(),
                seller.getBusinessName()
        );
    }

    /**
     * 판매자 계정의 이용 가능 상태를 검사한다.
     *
     * ACTIVE 상태인 판매자만 로그인할 수 있다.
     */
    private void validateSellerStatus(Seller seller) {

        SellerStatus status = seller.getStatus();

        // 정상 영업 상태라면 로그인 처리를 계속 진행한다.
        if (status == SellerStatus.ACTIVE) {
            return;
        }

        // 관리자가 일시 정지한 판매자 계정
        if (status == SellerStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.SELLER_SUSPENDED);
        }

        // 폐점 또는 강제 폐점된 계정
        if (
                status == SellerStatus.CLOSED
                        || status == SellerStatus.FORCED_CLOSED
        ) {
            throw new CustomException(ErrorCode.SELLER_NOT_APPROVED);
        }

        // 정의되지 않은 상태가 들어온 경우
        throw new CustomException(ErrorCode.INVALID_SELLER_STATUS);
    }
}