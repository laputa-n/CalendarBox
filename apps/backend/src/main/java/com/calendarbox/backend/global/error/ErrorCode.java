package com.calendarbox.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 공통
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다."),

    // 인증/인가
    AUTH_INVALID_TOKEN("AUTH_INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다."),
    AUTH_FORBIDDEN("AUTH_FORBIDDEN", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 카카오/회원 예시 (필요에 맞게 추가)
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    KAKAO_DUPLICATE_LINK("KAKAO_DUPLICATE_LINK", HttpStatus.CONFLICT, "이미 연결된 카카오 계정입니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String code() { return code; }
    public HttpStatus status() { return status; }
    public String message() { return message; }
}
