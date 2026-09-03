package com.shoppingmall.domain.auth.controller;

import com.shoppingmall.domain.user.entity.Role;
import com.shoppingmall.global.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [4-4 강화] 판매자 화면(seller-*.html)의 <b>서버측</b> 접근제어용 인가 엔드포인트.
 *
 * <p>정적 HTML 은 Nginx 가 인증과 무관하게 200 으로 내려주고, 화면 진입 여부는 브라우저의
 * JS(requireRole)가 판단해 왔다. 실데이터는 API 인가로 막혀 있어 노출은 없지만,
 * 프록시 도구에는 권한 없는 사용자의 요청도 {@code 200 OK} 로 기록된다.
 * Nginx {@code auth_request} 가 화면을 내려보내기 <b>전에</b> 이 엔드포인트를 서브요청으로
 * 호출해, 통과한 요청에만 HTML 을 준다.
 *
 * <p>응답은 <b>상태코드만</b> 의미가 있다(Nginx 는 본문을 사용하지 않는다).
 * <ul>
 *   <li>204 - 판매자 → Nginx 가 정적 파일을 그대로 응답</li>
 *   <li>401 - 비로그인 → Nginx 가 /login.html 로 302</li>
 *   <li>403 - 역할 불일치 → Nginx 가 /index.html 로 302</li>
 * </ul>
 *
 * <p>⚠️ 이 경로는 SecurityConfig 의 {@code /api/v1/auth/**} permitAll 에 포함된다.
 * 401 과 403 을 <b>여기서 직접</b> 구분해 내려야 Nginx 가 두 경우를 다른 화면으로 보낼 수 있다.
 * Security 단계에서 먼저 막으면 비로그인·역할불일치가 모두 401 로 뭉개진다.
 * 인증 자체는 JwtAuthenticationFilter 가 permitAll 경로에서도 쿠키를 읽어 principal 을 채우므로
 * 정상 동작한다.
 *
 * <p>⚠️ 외부 직접 호출은 Nginx 의 {@code internal} 지시자로 차단한다(애플리케이션은 관여하지 않는다).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class PageAuthzController {

    @GetMapping("/page-authz")
    public ResponseEntity<Void> pageAuthz(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (userDetails.getUser().getRole() != Role.SELLER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.noContent().build();
    }
}
