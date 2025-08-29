package com.calendarbox.backend.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[] messageArgs; // 선택적 자리표시자

    public BusinessException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.code());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
    }
    public ErrorCode getErrorCode() { return errorCode; }
    public Object[] getMessageArgs() { return messageArgs; }
}
