package com.shoppingmall.global.exception;

import com.shoppingmall.global.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 컨트롤러(도메인 무관)에서 던져지는 예외를 여기서 한 곳에 모아
 * ApiResponse 포맷으로 변환한다. 새 도메인을 추가해도 이 클래스는 손댈 필요 없음
 * (해당 도메인 서비스에서 CustomException + ErrorCode 조합만 던지면 됨).
 *
 * <p><b>[6-1 조치]</b> 어떤 핸들러도 예외 원문(필드 경로·클래스명·스택트레이스)을
 * 응답 본문에 담지 않는다. 원문은 서버 로그에만 남긴다.
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

    /**
     * [1-2 조치] 컨트롤러의 @RequestParam / @PathVariable 에 붙인 제약(@Size 등) 위반.
     * (@Validated 를 클래스에 붙였을 때 이 예외가 올라온다 — 없으면 500 으로 떨어진다)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        log.warn("Constraint violation: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(message));
    }

    /**
     * [1-6 조치] 요청 본문 형식 오류를 400 으로 처리한다.
     *
     * <p>JacksonConfig 에서 ACCEPT_FLOAT_AS_INT 를 껐기 때문에 {"quantity": 2.9} 같은
     * 요청이 여기로 온다. 이 핸들러가 없으면 500 으로 떨어진다.
     *
     * <p>⚠️ e.getMessage() 를 응답에 담지 말 것 — 필드명·클래스 경로가 노출되어 6-1 이 재발한다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문 형식 오류: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("요청 형식이 올바르지 않습니다."));
    }

    /** [2-1 조치] multipart 상한 초과 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("업로드 크기 초과: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.FILE_SIZE_EXCEEDED.getStatus())
                .body(ApiResponse.fail(ErrorCode.FILE_SIZE_EXCEEDED.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolationException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("이미 처리되었거나 중복된 요청입니다."));
    }

    /**
     * [6-1 조치] 없는 경로를 404 로 돌려준다.
     *
     * <p>이 핸들러가 없으면 아래 포괄 분기(Exception)가 잡아 <b>500</b> 을 낸다.
     * 없는 경로와 서버 장애가 같은 응답이 되어
     * <ul>
     *   <li>클라이언트가 "고장"과 "오타"를 구분하지 못하고,
     *   <li>정상 트래픽(오래된 북마크·스캐너)이 매번 {@code log.error} 로 쌓이며,
     *   <li>500 을 내는 경로와 200 을 내는 경로의 차이가 곧 <b>경로 존재 여부</b>가 되어
     *       탐색에 단서를 준다.
     * </ul>
     *
     * <p>Spring Boot 3.2+ 에서는 매핑되지 않은 요청이 정적 리소스 조회로 넘어가
     * {@code NoResourceFoundException} 이 된다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("존재하지 않는 경로: {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("요청하신 경로를 찾을 수 없습니다."));
    }

    /**
     * [6-1 조치] 경로는 있으나 메서드가 다른 경우를 405 로 돌려준다.
     *
     * <p>404 와 마찬가지로 두지 않으면 500 이 된다.
     * 어떤 메서드가 허용되는지는 응답 본문에 싣지 않는다(6-1).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("허용되지 않은 메서드: {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail("허용되지 않은 요청 방식입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
