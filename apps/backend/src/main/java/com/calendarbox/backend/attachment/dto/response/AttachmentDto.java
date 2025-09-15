package com.calendarbox.backend.attachment.dto.response;

public record AttachmentDto(Long id, String name, String mimeType, long size, int position) {}
