package com.shoppingmall.global.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingmall.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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

    /** 토큰은 유효하지만 권한이 없을 때 (403) - 예: USER가 /seller 나 /admin API 호출 */
    @Component
    public static class DeniedHandler implements AccessDeniedHandler {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                            AccessDeniedException accessDeniedException) throws IOException {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "접근 권한이 없습니다.", objectMapper);
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
