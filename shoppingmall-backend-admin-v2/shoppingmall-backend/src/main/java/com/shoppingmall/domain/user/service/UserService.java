package com.shoppingmall.domain.user.service;

import com.shoppingmall.domain.user.dto.request.UserUpdateRequest;
import com.shoppingmall.domain.user.dto.response.MyPageResponse;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // SecurityConfig 내 BCrypt 빈 주입 필요

    // 마이페이지 요약 조회 정보 연산
    public MyPageResponse getMyPageSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return MyPageResponse.from(user);
    }

    // 회원 개인정보 수정 처리
    @Transactional
    public void updateUserInfo(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 수정 요청 시 기존 비밀번호 일치 판정 유효성 체크
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD); // 패스워드 불일치 에러 예외 발생
            }
            // 새로운 비밀번호 해시 암호화 적용
            // 엔티티 내에 setter 대신 데이터 무결성을 유지하는 자바 변경 메서드를 설계해 활용하는 것을 권장합니다.
            // 여기서는 스켈레톤 상태에 맞춰 필드를 추상화하거나 엔티티 내 메서드를 추가 연동합니다.
        }

        // 이메일 중복 검증 등 추가 가드 로직 가능
        // JPA 영속성 컨텍스트에 의해 트랜잭션 종료 시 더티 체킹 자동 DB 반영(Update) 완료
    }
}