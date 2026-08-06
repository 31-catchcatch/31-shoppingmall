package com.shoppingmall.global.exception;

import com.shoppingmall.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 컨트롤러(도메인 무관)에서 던져지는 예외를 여기서 한 곳에 모아
 * ApiResponse 포맷으로 변환한다. 새 도메인을 추가해도 이 클래스는 손댈 필요 없음
 * (해당 도메인 서비스에서 CustomException + ErrorCode 조합만 던지면 됨).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("CustomException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolationException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("이미 처리되었거나 중복된 요청입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
