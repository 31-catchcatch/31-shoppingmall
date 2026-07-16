package com.shoppingmall.domain.auth.controller;

import com.shoppingmall.domain.auth.dto.request.*;
import com.shoppingmall.domain.auth.dto.response.*;
import com.shoppingmall.domain.auth.service.AccountRecoveryService;
import com.shoppingmall.domain.auth.service.AuthService;
import com.shoppingmall.domain.auth.service.EmailVerificationService;
import com.shoppingmall.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API 명세서 "공통/인증 - 인증" 도메인 전체 매핑.
 * 실제 서비스 로직은 각 전용 Service 로 위임하고, 이 컨트롤러는 URL 라우팅 + 요청/응답 변환만 담당.
 *
 * 판매자 로그인(/auth/seller/login)은 domain.seller.controller.SellerAuthController 가 담당한다
 * (Seller/SellerApplication 분리 구조를 아는 seller 도메인 쪽에 있는 게 더 자연스러워서 그쪽에 남겨둠 -
 *  같은 URL을 여기서 또 매핑하면 "Ambiguous mapping" 에러가 나므로 절대 여기서 중복 매핑하면 안 됨).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final AccountRecoveryService accountRecoveryService;

    // ===== 일반 사용자 =====

    @PostMapping("/user/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    @PostMapping("/user/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/user/verify")
    public ResponseEntity<ApiResponse<VerifyResponse>> verifyUser(@Valid @RequestBody VerifyRequest request) {
        // TODO(실제 연동 필요): SMS/PASS 등 본인인증 업체 연동 전까지는 항상 성공 처리하는 mock
        return ResponseEntity.ok(ApiResponse.success(VerifyResponse.mockSuccess()));
    }

    @PostMapping("/user/find-account")
    public ResponseEntity<ApiResponse<FindAccountResponse>> findUserAccount(@Valid @RequestBody FindAccountRequest request) {
        FindAccountResponse response = request.type() == FindAccountRequest.FindAccountType.ID
                ? accountRecoveryService.findUsername(request)
                : accountRecoveryService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/user/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다.", null));
    }

    // ===== 판매자 =====
    // 판매자 회원가입(/seller/signup)과 로그인(/seller/login)은 모두
    // domain.seller.controller.SellerAuthController 가 전담한다 (위 클래스 주석 참고).

    @PostMapping("/seller/verify")
    public ResponseEntity<ApiResponse<VerifyResponse>> verifySeller(@Valid @RequestBody VerifyRequest request) {
        // TODO(실제 연동 필요): 위 user/verify 와 동일한 mock
        return ResponseEntity.ok(ApiResponse.success(VerifyResponse.mockSuccess()));
    }

    @PostMapping("/seller/find-account")
    public ResponseEntity<ApiResponse<FindAccountResponse>> findSellerAccount(@Valid @RequestBody FindAccountRequest request) {
        FindAccountResponse response = request.type() == FindAccountRequest.FindAccountType.ID
                ? accountRecoveryService.findUsername(request)
                : accountRecoveryService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== 공용 =====

    /** 프론트(find-account.js) 경로에 맞춘 아이디 찾기. 기존 /user/find-account(type=ID)와 동일 로직 */
    @PostMapping("/find-username")
    public ResponseEntity<ApiResponse<FindAccountResponse>> findUsername(
            @Valid @RequestBody FindUsernameRequest request) {
        return ResponseEntity.ok(ApiResponse.success(accountRecoveryService.findUsername(request)));
    }

    /** 프론트(find-account.js) 경로에 맞춘 비밀번호 재설정. 이메일 인증 통과 후 새 비밀번호를 직접 지정 */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        accountRecoveryService.resetPasswordDirect(request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.", null));
    }

    @PostMapping("/check-username")
    public ResponseEntity<ApiResponse<Boolean>> checkUsername(@RequestParam String username) {
        return ResponseEntity.ok(ApiResponse.success(authService.isUsernameAvailable(username)));
    }

    @PostMapping("/email-verification")
    public ResponseEntity<ApiResponse<EmailVerificationResponse>> emailVerification(
            @Valid @RequestBody EmailVerificationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(emailVerificationService.handle(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request.refreshToken())));
    }
}
