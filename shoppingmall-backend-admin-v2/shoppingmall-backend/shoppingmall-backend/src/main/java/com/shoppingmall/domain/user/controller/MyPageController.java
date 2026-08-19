package com.shoppingmall.domain.user.controller;

import com.shoppingmall.domain.user.dto.request.UserUpdateRequest;
import com.shoppingmall.domain.user.dto.response.MyPageResponse;
import com.shoppingmall.domain.user.service.UserService;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class MyPageController {

    private final UserService userService;

    // 1. 마이페이지 요약 정보 조회 (성공 시 MyPageResponse 데이터 반환)
    @GetMapping
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MyPageResponse response = userService.getMyPageSummary(userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success("마이페이지 정보 조회가 완료되었습니다.", response));
    }

    // 2. 회원 정보 수정 (성공 시 Void 반환하므로 두 번째 인자에 null 기입)
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateRequest request) {
        userService.updateUserInfo(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(ApiResponse.success("회원 정보가 성공적으로 변경되었습니다.", null));
    }
}