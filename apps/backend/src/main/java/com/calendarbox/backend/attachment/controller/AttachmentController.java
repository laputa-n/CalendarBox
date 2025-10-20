package com.calendarbox.backend.attachment.controller;

import com.calendarbox.backend.attachment.service.AttachmentService;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.attachment.dto.response.FileAttachmentDto;
import com.calendarbox.backend.attachment.dto.response.ImageAttachmentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    /** 일정의 이미지(갤러리) */
    @GetMapping("/schedules/{scheduleId}/attachments/images")
    public ResponseEntity<ApiResponse<List<ImageAttachmentDto>>> images(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ) {

        var data = attachmentService.getImages(userId, scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("이미지 첨부파일 목록 조회를 성공하였습니다",data));
    }

    /** 일정의 비이미지(파일 리스트) */
    @GetMapping("/schedules/{scheduleId}/attachments/files")
    public ResponseEntity<ApiResponse<List<FileAttachmentDto>>> files(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ) {
        var data = attachmentService.getFiles(userId,scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("비이미지 첨부파일 조회를 성공했습니다.",data));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<String> download(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "false") boolean inline
    ) {
        String url = attachmentService.getDownloadUrl(userId, attachmentId, inline);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(url);
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long attachmentId
    ) {
        attachmentService.deleteAttachment(userId, attachmentId);
        return ResponseEntity.noContent().build(); // 204
    }
}

