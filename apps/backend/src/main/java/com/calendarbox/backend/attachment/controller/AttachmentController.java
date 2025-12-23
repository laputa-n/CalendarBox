package com.calendarbox.backend.attachment.controller;

import com.calendarbox.backend.attachment.service.AttachmentService;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.attachment.dto.response.FileAttachmentDto;
import com.calendarbox.backend.attachment.dto.response.ImageAttachmentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Schedule - Attachment", description = "스케줄 첨부 파일")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    /** 일정의 이미지(갤러리) */
    @Operation(
            summary = "첨부 파일 조회(이미지)",
            description = "해당 스케줄의 이미지 첨부 파일을 조회합니다."
    )
    @GetMapping("/schedules/{scheduleId}/attachments/images")
    public ResponseEntity<ApiResponse<List<ImageAttachmentDto>>> images(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ) {

        var data = attachmentService.getImages(userId, scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("이미지 첨부파일 목록 조회를 성공하였습니다",data));
    }

    /** 일정의 비이미지(파일 리스트) */
    @Operation(
            summary = "첨부 파일 조회(비이미지)",
            description = "해당 스케줄의 비이미지 첨부 파일을 조회합니다."
    )
    @GetMapping("/schedules/{scheduleId}/attachments/files")
    public ResponseEntity<ApiResponse<List<FileAttachmentDto>>> files(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ) {
        var data = attachmentService.getFiles(userId,scheduleId);

        return ResponseEntity.ok(ApiResponse.ok("비이미지 첨부파일 조회를 성공했습니다.",data));
    }

    @Operation(
            summary = "첨부 파일 다운로드",
            description = "첨부 파일을 다운로드 합니다."
    )
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

    @Operation(
            summary = "첨부 파일 삭제",
            description = "해당 첨부 파일을 삭제합니다."
    )
    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long attachmentId
    ) {
        attachmentService.deleteAttachment(userId, attachmentId);
        return ResponseEntity.noContent().build(); // 204
    }
}

