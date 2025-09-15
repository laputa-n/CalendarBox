package com.calendarbox.backend.attachment.dto.response;

public record FileAttachmentDto(
        Long id,
        String name,
        String mimeType,
        long size,
        String downloadUrl // 다운로드 presign
) {}
