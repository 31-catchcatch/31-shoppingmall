package com.shoppingmall.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingmall.global.common.ApiResponse;
import com.shoppingmall.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

public class JwtExceptionHandlers {

    /** 토큰이 없거나 유효하지 않을 때 (401) */
    @Component
    public static class EntryPoint implements AuthenticationEntryPoint {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                              AuthenticationException authException) throws IOException {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다.", objectMapper);
        }
    }

    /**
     * 토큰은 유효하지만 권한이 없을 때 (403) - 예: USER가 /seller 나 /admin API 호출
     *
     * <p>[4-1 조치] CSRF 실패도 같은 403 으로 떨어진다. 메시지가 같으면 운영 중에
     * 원인을 구분할 수 없어, {@link CsrfException} 만 따로 잡아 다른 문구를 준다.
     * 프론트({@code admin-api.js})는 403 을 로그아웃으로 처리하지 않고 이 메시지를 그대로 띄운다.
     */
    @Component
    public static class DeniedHandler implements AccessDeniedHandler {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                            AccessDeniedException accessDeniedException) throws IOException {
            String message = (accessDeniedException instanceof CsrfException)
                    ? ErrorCode.CSRF_TOKEN_INVALID.getMessage()
                    : "접근 권한이 없습니다.";
            writeError(response, HttpServletResponse.SC_FORBIDDEN, message, objectMapper);
        }
    }

    private static void writeError(HttpServletResponse response, int status, String message,
                                    ObjectMapper objectMapper) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(message)));
    }
}
