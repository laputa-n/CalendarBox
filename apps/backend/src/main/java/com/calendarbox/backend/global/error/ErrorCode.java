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
    KAKAO_DUPLICATE_LINK("KAKAO_DUPLICATE_LINK", HttpStatus.CONFLICT, "이미 연결된 카카오 계정입니다."),


    INVALID_JSON("INVALID_JSON", HttpStatus.BAD_REQUEST, "요청 JSON 파싱에서 오류가 발생했습니다."),

    FRIENDSHIP_SELF_REQUEST("FRIENDSHIP_SELF_REQUEST", HttpStatus.BAD_REQUEST, "자기 자신에게는 친구 요청을 보낼 수 없습니다."),
    FRIENDSHIP_INVALID_STATE("FRIENDSHIP_INVALID_STATE", HttpStatus.CONFLICT, "요청 상태가 유효하지 않아 처리할 수 없습니다. (현재: {0})"),
    FRIENDSHIP_ALREADY_RESPONDED("FRIENDSHIP_ALREADY_RESPONDED", HttpStatus.CONFLICT, "이미 응답된 요청입니다."),
    FRIENDSHIP_REQUIRED("FRIENDSHIP_REQUIRED",HttpStatus.FORBIDDEN,"친구가 아닙니다."),

    CALENDAR_NAME_DUPLICATE("CALENDAR_NAME_DUPLICATE", HttpStatus.CONFLICT,"이미 동일한 캘린더를 보유하고 있습니다."),
    CALENDAR_NOT_FOUND("CALENDAR_NOT_FOUND", HttpStatus.NOT_FOUND, "해당 캘린더가 존재하지 않습니다."),

    REQUEST_NO_CHANGES("REQUEST_NO_CHANGES", HttpStatus.BAD_REQUEST,"변화가 없습니다."),
    DEFAULT_ONLY_FOR_PERSONAL("DEFAULT_ONLY_FOR_PERSONAL", HttpStatus.BAD_REQUEST, "개인 캘린더만 기본 캘린더로 설정이 가능합니다."),
    INVITE_ONLY_FOR_GROUP("INVITE_ONLY_FOR_GROUP", HttpStatus.BAD_REQUEST,"그룹 캘린더만 멤버 초대가 가능합니다."),
    REINVITE_NOT_ALLOWED("REINVITE_NOT_ALLOWED", HttpStatus.BAD_REQUEST,"재초대가 불가합니다.");
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
