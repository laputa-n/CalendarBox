package com.calendarbox.backend.attachment.dto.response;

public record PresignResponse(String uploadId, String objectKey, String presignedUrl) {}
