package com.shoppingmall.domain.user.service;

import com.shoppingmall.domain.user.dto.request.UserUpdateRequest;
import com.shoppingmall.domain.user.dto.response.MyPageResponse;
import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.auth.service.AuthService;
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
    private final AuthService authService;         // [4-2 조치] 비밀번호 변경 시 세션 종료

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

            // [4-2 조치] 비밀번호를 바꾸면 기존 세션을 전부 끊는다.
            // 탈취된 세션을 사용자가 스스로 끊을 수 있어야 하고, 그게 비밀번호 변경의 기대 동작이다.
            // 본인의 현재 기기도 함께 로그아웃되며 재로그인이 필요하다(의도된 동작).
            authService.terminateAllSessions(userId);
        }

        // 이름·이메일·전화번호 반영 → 트랜잭션 종료 시 더티 체킹으로 UPDATE
        user.updateProfile(request.getName(), request.getEmail(), request.getPhoneNumber());
    }
}