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

        // 새 비밀번호 요청이 있을 때만 현재 비밀번호 검증 후 변경
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
            user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        }

        // 이름·이메일·전화번호 반영 → 트랜잭션 종료 시 더티 체킹으로 UPDATE
        user.updateProfile(request.getName(), request.getEmail(), request.getPhoneNumber());
    }
}