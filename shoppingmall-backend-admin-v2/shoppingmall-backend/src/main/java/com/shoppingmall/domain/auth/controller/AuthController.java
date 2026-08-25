package com.shoppingmall.domain.auth.controller;

import com.shoppingmall.domain.auth.dto.request.*;
import com.shoppingmall.domain.auth.dto.response.*;
import com.shoppingmall.domain.auth.service.AccountRecoveryService;
import com.shoppingmall.domain.auth.service.AuthService;
import com.shoppingmall.domain.auth.service.EmailVerificationService;
import com.shoppingmall.domain.auth.dto.request.VerifyAccountRequest;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.exception.CustomException;
import com.shoppingmall.global.exception.ErrorCode;
import com.shoppingmall.global.security.CustomUserDetails;
import com.shoppingmall.global.security.cookie.AuthCookieFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final AuthCookieFactory authCookieFactory;   // [4-1 조치]

    // ===== 일반 사용자 =====

    @PostMapping("/user/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    /**
     * [4-1 조치] 로그인 성공 시 토큰을 HttpOnly 쿠키로 내려보낸다.
     *
     * <p>[4-1 조치 · 3단계] 응답 본문에서 토큰을 제거했다. 인증은 Set-Cookie 로만 전달된다.
     * 본문에는 화면이 곧바로 쓸 최소 식별정보(userId·role·name)만 남긴다.
     */
    @PostMapping("/user/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletResponse response) {
        TokenResponse tokens = authService.login(request);
        authCookieFactory.writeLoginCookies(response, tokens.accessToken(), tokens.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(tokens)));
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

    /**
     * [4-2 조치] 로그아웃 — 토큰 주인을 Authorization 헤더로 식별해 발급 토큰을 무효화한다.
     *
     * <p>본문의 refreshToken 을 받던 방식은 프론트가 refreshToken 을 보관하지 않게 되면서
     * 쓸 수 없다. 구버전 클라이언트가 보내오는 경우에만 부가적으로 처리한다.
     * 토큰이 없거나 만료된 요청도 200 으로 응답한다(멱등).
     */
    @PostMapping("/user/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        // [4-1 조치] 인증 실패 여부와 무관하게 항상 쿠키를 만료시킨다.
        // 로그아웃은 멱등해야 하고, 망가진 쿠키를 사용자가 스스로 털어낼 수 있어야 한다.
        authCookieFactory.clearAuthCookies(response);

        authService.logout(userDetails == null ? null : userDetails.getUser().getId());

        // [4-2 조치] 액세스 토큰이 이미 만료된 상태로 로그아웃하면 principal 이 null 이라
        // 위 호출이 아무 일도 하지 않는다. 그 경우 DB 의 리프레시 토큰이 만료(7일)까지 남으므로,
        // 리프레시 쿠키를 신뢰 가능한 식별자로 함께 사용한다.
        authCookieFactory.readRefreshToken(httpRequest)
                .ifPresent(authService::logoutByRefreshToken);

        if (request != null) {
            authService.logoutByRefreshToken(request.refreshToken());
        }
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
    @PostMapping("/verify-account")
    public ResponseEntity<ApiResponse<Void>> verifyAccount(
            @Valid @RequestBody VerifyAccountRequest request) {
        accountRecoveryService.verifyAccount(request);
        return ResponseEntity.ok(
                ApiResponse.success("확인되었습니다. 새 비밀번호를 입력해 주세요.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        accountRecoveryService.resetPasswordDirect(request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.", null));
    }

    /**
     * [4-1 조치] CSRF 토큰 프라이밍용 엔드포인트.
     *
     * <p>화면 HTML 은 Nginx 가 직접 서빙하므로 로그인 전에는 브라우저가 백엔드를 한 번도
     * 거치지 않는다. 그래서 XSRF-TOKEN 쿠키가 없는 상태로 첫 요청을 보내게 되는데,
     * 프론트가 이 경로를 먼저 호출하면 쿠키를 확보한 뒤 진행할 수 있다.
     */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
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

    /**
     * [4-1 조치] 토큰 재발급 — 리프레시 토큰을 쿠키에서 읽는다.
     *
     * <p>본문(RefreshRequest)은 전환기 호환용 폴백이며 3단계에서 제거한다.
     * {@code @Valid} 와 {@code @NotBlank} 를 그대로 두면 쿠키만 있고 본문이 없는 정상 요청이
     * 400 으로 죽으므로 검증을 걷어냈다.
     *
     * <p><b>회전(rotation) 대응</b>: {@code AuthService.refresh()} 는 기존 리프레시 토큰을
     * 삭제하고 새로 발급한다. 새 쿠키를 다시 굽지 않으면 브라우저가 옛 값을 계속 들고 있다가
     * 두 번째 재발급에서 조회 실패로 로그아웃된다("15분은 되는데 30분이면 끊긴다").
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String refreshToken = authCookieFactory.readRefreshToken(httpRequest)
                .orElseGet(() -> request == null ? null : request.refreshToken());

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        TokenResponse tokens = authService.refresh(refreshToken);
        authCookieFactory.writeLoginCookies(response, tokens.accessToken(), tokens.refreshToken());

        // [4-1 조치 · 3단계] 재발급 결과는 쿠키로만 전달한다. 본문은 비운다(204).
        // 프론트(js/http.js)는 응답 상태만 보고 원요청을 재시도한다.
        return ResponseEntity.noContent().build();
    }
}
