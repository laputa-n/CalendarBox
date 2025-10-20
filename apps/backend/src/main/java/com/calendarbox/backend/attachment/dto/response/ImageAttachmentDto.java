package com.calendarbox.backend.attachment.dto.response;

public record ImageAttachmentDto(
        Long id,
        String name,
        String mimeType,
        long size,
        int position,
        String thumbUrl,   // 갤러리 썸네일(없으면 원본 presign으로 대체해도 OK)
        String imageUrl    // 원본 이미지(모달/확대용)
) {}
