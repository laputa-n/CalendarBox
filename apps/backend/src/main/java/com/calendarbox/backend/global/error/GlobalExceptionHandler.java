package com.calendarbox.backend.global.error;

import com.calendarbox.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.calendarbox.backend.global.error.ErrorBody;

import java.text.MessageFormat;
import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    private static String fmt(String template, Object[] args) {
        return (args == null || args.length == 0) ? template : MessageFormat.format(template, args);
    }

    // 1) Bean Validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String first = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst().orElse(ErrorCode.VALIDATION_ERROR.message());

        ErrorCode ec = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(ec.status())
                .body(new ErrorBody(ec.code(), first, Instant.now(), req.getRequestURI()));
    }

    // 2) 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorBody> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorCode ec = ex.getErrorCode();
        String msg = fmt(ec.message(), ex.getMessageArgs());
        return ResponseEntity.status(ec.status())
                .body(new ErrorBody(ec.code(), msg, Instant.now(), req.getRequestURI()));
    }

    // 3) 인증/인가 (필요 시 매핑)
    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<ErrorBody> handleJwt(io.jsonwebtoken.JwtException ex, HttpServletRequest req) {
        log.error("JWT Error", ex); // 에러 로그 보기 위해
        ErrorCode ec = ErrorCode.AUTH_INVALID_TOKEN;
        return ResponseEntity.status(ec.status())
                .body(new ErrorBody(ec.code(), ec.message(), Instant.now(), req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.AUTH_FORBIDDEN;
        return ResponseEntity.status(ec.status())
                .body(new ErrorBody(ec.code(), ec.message(), Instant.now(), req.getRequestURI()));
    }

    // 4) 잘못된 인자 → 400으로 통일
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> handleIAE(IllegalArgumentException ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity.status(ec.status())
                .body(new ErrorBody(ec.code(), ex.getMessage(), Instant.now(), req.getRequestURI()));
    }


    // 5) 나머지 전부
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleAll(Exception ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(ec.status())
                .body(new ErrorBody(ec.code(), ec.message(), Instant.now(), req.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> handleJsonParse(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INVALID_JSON;
        return ResponseEntity.status(ec.status())
                .body(new ErrorBody(ec.code(), ec.message(), Instant.now(), req.getRequestURI()));
    }

}
