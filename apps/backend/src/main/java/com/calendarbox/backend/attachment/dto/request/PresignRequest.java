package com.calendarbox.backend.attachment.dto.request;

public record PresignRequest(
        Long scheduleId, String filename, String contentType, long size
) {}
