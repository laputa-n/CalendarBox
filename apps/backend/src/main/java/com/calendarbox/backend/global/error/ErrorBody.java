package com.calendarbox.backend.global.error;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ErrorBody", description = "공통 에러 응답 바디")
public record ErrorBody(
        String code,
        String message,
        Instant timestamp,
        String path
) {}
