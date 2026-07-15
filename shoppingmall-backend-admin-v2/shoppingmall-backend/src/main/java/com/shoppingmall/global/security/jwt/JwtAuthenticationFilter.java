package com.shoppingmall.global.security.jwt;

import com.shoppingmall.domain.user.entity.User;
import com.shoppingmall.domain.user.repository.UserRepository;
import com.shoppingmall.global.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * API 명세서 상 "마이페이지(/me) 및 권한이 필요한 API는 Authorization 헤더의 JWT로 식별" 규칙을 구현.
 * 인증이 필요 없는 API(상품 리스트, 로그인/회원가입 등)는 SecurityConfig 에서 permitAll 처리하고
 * 이 필터는 통과만 시킨다.
 *
 * principal은 CustomUserDetails로 감싸서 넣는다 (Long userId를 그대로 넣지 않음).
 * 컨트롤러에서 @AuthenticationPrincipal CustomUserDetails userDetails 로 받아
 * userDetails.getUser().getId() 형태로 꺼내 쓰는 게 이 프로젝트의 표준 패턴이다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserId(token);

            Optional<User> userOptional = userRepository.findById(userId);
            // 탈퇴(soft delete) 등으로 토큰은 유효하지만 사용자가 사라진 경우 인증 미적용 -> 401 처리됨
            if (userOptional.isPresent() && !userOptional.get().isDeleted()) {
                CustomUserDetails userDetails = new CustomUserDetails(userOptional.get());
                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(PREFIX)) {
            return bearer.substring(PREFIX.length());
        }
        return null;
    }
}
