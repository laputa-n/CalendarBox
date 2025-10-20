package com.calendarbox.backend.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 공통
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다."),
    EXTERNAL_API_ERROR("EXTERNAL_API_ERROR", HttpStatus.BAD_GATEWAY,"외부 API 연동 중 오류가 발생했습니다."),
    // 인증/인가
    NOT_LOGGED_IN("NOT_LOGGED_IN", HttpStatus.NOT_FOUND,"로그인이 되어있지 않습니다."),
    AUTH_INVALID_TOKEN("AUTH_INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않습니다."),
    AUTH_FORBIDDEN("AUTH_FORBIDDEN", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 카카오/회원 예시 (필요에 맞게 추가)
    MEMBER_NOT_FOUND("MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    KAKAO_DUPLICATE_LINK("KAKAO_DUPLICATE_LINK", HttpStatus.CONFLICT, "이미 연결된 카카오 계정입니다."),


    INVALID_JSON("INVALID_JSON", HttpStatus.BAD_REQUEST, "요청 JSON 파싱에서 오류가 발생했습니다."),
    START_AFTER_BEFORE("START_AFTER_BEFORE", HttpStatus.BAD_REQUEST,"시작 시간이 종료 시간 이후입니다"),
    FRIENDSHIP_SELF_REQUEST("FRIENDSHIP_SELF_REQUEST", HttpStatus.BAD_REQUEST, "자기 자신에게는 친구 요청을 보낼 수 없습니다."),
    FRIENDSHIP_INVALID_STATE("FRIENDSHIP_INVALID_STATE", HttpStatus.CONFLICT, "요청 상태가 유효하지 않아 처리할 수 없습니다. (현재: {0})"),
    FRIENDSHIP_ALREADY_RESPONDED("FRIENDSHIP_ALREADY_RESPONDED", HttpStatus.CONFLICT, "이미 응답된 요청입니다."),
    FRIENDSHIP_REQUIRED("FRIENDSHIP_REQUIRED",HttpStatus.FORBIDDEN,"친구가 아닙니다."),
    FRIENDSHIP_NOT_FOUND("FRIENDSHIP_NOT_FOUNT",HttpStatus.NOT_FOUND,"친구 관계를 찾을 수 없습니다."),

    CALENDAR_NAME_DUPLICATE("CALENDAR_NAME_DUPLICATE", HttpStatus.CONFLICT,"이미 동일한 캘린더를 보유하고 있습니다."),
    CALENDAR_NOT_FOUND("CALENDAR_NOT_FOUND", HttpStatus.NOT_FOUND, "해당 캘린더가 존재하지 않습니다."),
    CALENDAR_MEMBER_NOT_FOUND("CALENDAR_MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND, "해당 캘린더 멤버 초대가 존재하지 않습니다."),
    CALENDAR_MEMBER_ALREADY_RESPONDED("CALENDAR_MEMBER_ALREADY_RESPONDED", HttpStatus.CONFLICT, "이미 응답된 요청입니다."),

    REQUEST_NO_CHANGES("REQUEST_NO_CHANGES", HttpStatus.BAD_REQUEST,"변화가 없습니다."),
    DEFAULT_ONLY_FOR_PERSONAL("DEFAULT_ONLY_FOR_PERSONAL", HttpStatus.BAD_REQUEST, "개인 캘린더만 기본 캘린더로 설정이 가능합니다."),
    INVITE_ONLY_FOR_GROUP("INVITE_ONLY_FOR_GROUP", HttpStatus.BAD_REQUEST,"그룹 캘린더만 멤버 초대가 가능합니다."),
    REINVITE_NOT_ALLOWED("REINVITE_NOT_ALLOWED", HttpStatus.BAD_REQUEST,"재초대가 불가합니다."),

    ATTACHMENT_NOT_FOUND("ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    ATTACHMENT_TYPE_NOT_SUPPORTED("ATTACHMENT_TYPE_NOT_SUPPORTED",HttpStatus.BAD_REQUEST,"지원하지 않는 첨부 파일 타입입니다."),
    ATTACHMENT_SIZE_NOT_INVALID("ATTACHMENT_SIZE_NOT_INVALID",HttpStatus.BAD_REQUEST,"첨부 파일의 크기가 너무 큽니다."),
    ATTACHMENT_NOT_CACHED("ATTACHMENT_NOT_CACHED", HttpStatus.NOT_FOUND,"첨부파일의 캐시를 찾을 수 없습니다."),
    SCHEDULE_NOT_FOUND("SCHEDULE_NOT_FOUND", HttpStatus.NOT_FOUND, "스케줄을 찾을 수 없습니다."),
    SCHEDULE_TODO_NOT_MATCH("SCHEDULE_TODO_NOT_MATCH",HttpStatus.CONFLICT,"투두가 스케줄에 속하지 않습니다."),
    SCHEDULE_PLACE_NOT_MATCH("SCHEDULE_PLACE_NOT_MATCH",HttpStatus.CONFLICT,"장소가 스케줄에 속하지 않습니다."),
    TODO_NOT_FOUND("TODO_NOT_FOUND",HttpStatus.NOT_FOUND,"투두가 존재하지 않습니다."),

    RECURRENCE_NOT_FOUND("RECURRENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "반복 규칙이 존재하지 않습니다."),
    RECURRENCE_EXDATE_DUP("RECURRENCE_EXDATE_DUP", HttpStatus.CONFLICT, "예외 날짜가 이미 존재합니다."),
    RECURRENCE_EXDATE_NOT_FOUND("RECURRENCE_EXDATE_NOT_FOUND", HttpStatus.NOT_FOUND, "예외 날짜가 존재하지 않습니다."),
    SCHEDULE_RECUR_EXDATE_MISMATCH("SCHEDULE_RECUR_EXDATE_MISMATCH", HttpStatus.BAD_REQUEST, "예외 날짜가 해당 스케줄 반복에 속하지 않습니다."),
    RECURRENCE_UNTIL_BEFORE_END("RECURRENCE_UNTIL_BEFORE_END", HttpStatus.BAD_REQUEST, "반복 종료일은 스케줄 종료일 이후여야 합니다."),
    RECURRENCE_ALREADY_EXISTS("RECURRENCE_ALREADY_EXISTS", HttpStatus.CONFLICT,"예외가 이미 존재합니다."),

    REMINDER_MINUTES_DUP("REMINDER_MINUTES_DUP", HttpStatus.CONFLICT,"리마인더가 이미 존재합니다."),
    REMINDER_NOT_FOUND("REMINDER_NOT_FOUND", HttpStatus.NOT_FOUND, "리마인더가 존재하지 않습니다"),

    SCHEDULE_PLACE_NAME_NEED("SCHEDULE_PLACE_NAME_NEED", HttpStatus.BAD_REQUEST,"스케줄 장소 이름이 필요합니다."),
    SCHEDULE_PLACE_NAME_DUP("SCHEDULE_PLACE_NAME_DUP", HttpStatus.CONFLICT, "동일한 스케줄 장소 이름이 이미 존재합니다."),
    SCHEDULE_PLACE_DUP("SCHEDULE_PLACE_DUP", HttpStatus.CONFLICT, "동일한 스케줄 장소가 이미 등록되어 있습니다."),
    EXISTING_PLACE_ID_NEED("EXISTING_PLACE_ID_NEED", HttpStatus.BAD_REQUEST,"EXISTING은 PLACEID가 필요합니다."),

    PLACE_NOT_FOUND("PLACE_NOT_FOUND",HttpStatus.NOT_FOUND,"장소를 찾을 수 없습니다."),
    SCHEDULE_PLACE_NOT_FOUND("SCHEDULE_PLACE_NOT_FOUND",HttpStatus.NOT_FOUND,"일정 장소를 찾을 수 없습니다."),

    SCHEDULE_PARTICIPANT_NOT_FOUND("SCHEDULE_PARTICIPANT_NOT_FOUND", HttpStatus.NOT_FOUND,"일정 참가자를 찾을 수 없습니다"),
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", HttpStatus.NOT_FOUND,"해당 알림을 찾을 수 없습니다"),

    SCHEDULE_LINK_ALREADY_EXISTS("SCHEDULE_LINK_ALREADY_EXISTS", HttpStatus.CONFLICT,"해당 링크가 이미 존재합니다."),
    SCHEDULE_LINK_LABEL_DUP("SCHEDULE_LINK_LABEL_DUP", HttpStatus.CONFLICT,"링크 라벨이 중복입니다."),
    SCHEDULE_LINK_NOT_FOUND("SCHEDULE_LINK_NOT_FOUND", HttpStatus.NOT_FOUND, "링크를 찾을 수 없습니다.");

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
