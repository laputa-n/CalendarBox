package com.calendarbox.backend.attachment.controller;

import com.calendarbox.backend.attachment.service.AttachmentService;
import com.calendarbox.backend.global.dto.ApiResponse;
import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.attachment.dto.response.FileAttachmentDto;
import com.calendarbox.backend.attachment.dto.response.ImageAttachmentDto;
import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.global.infra.storage.StorageClient;
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

    private final AttachmentRepository attachmentRepository;
    private final AttachmentService attachmentService;
    private final StorageClient storage;

    /** 일정의 이미지(갤러리) */
    @GetMapping("/schedules/{scheduleId}/attachments/images")
    public ResponseEntity<ApiResponse<List<ImageAttachmentDto>>> images(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ) {
        // TODO: 권한검사 authz.checkCanReadSchedule(userId, sid);

        List<Attachment> list = attachmentRepository.findImagesByScheduleId(scheduleId);
        List<ImageAttachmentDto> dto = list.stream().map(a -> {
            String thumbKey = storage.toThumbKey(a.getObjectKey());
            String thumbUrl = storage.presignGet(thumbKey, a.getOriginalName(), true);
            String imageUrl = storage.presignGet(a.getObjectKey(), a.getOriginalName(), true);
            return new ImageAttachmentDto(
                    a.getId(),
                    a.getOriginalName(),
                    a.getMimeType(),
                    a.getByteSize(),
                    a.getPosition(),
                    thumbUrl,
                    imageUrl
            );
        }).toList();

        return ResponseEntity.ok(ApiResponse.ok("이미지 첨부파일 목록 조회를 성공하였습니다",dto));
    }

    /** 일정의 비이미지(파일 리스트) */
    @GetMapping("/schedules/{scheduleId}/attachments/files")
    public ResponseEntity<ApiResponse<List<FileAttachmentDto>>> files(
            @AuthenticationPrincipal(expression = "id") Long userId,
            @PathVariable Long scheduleId
    ) {
        // TODO: 권한검사 authz.checkCanReadSchedule(userId, sid);

        var dto = attachmentRepository.findFilesByScheduleId(scheduleId).stream().map(a ->
                new FileAttachmentDto(
                        a.getId(),
                        a.getOriginalName(),
                        a.getMimeType(),
                        a.getByteSize(),
                        storage.presignGet(a.getObjectKey(), a.getOriginalName(), false)
                )
        ).toList();

        return ResponseEntity.ok(ApiResponse.ok("비이미지 첨부파일 조회를 성공했습니다.",dto));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<String> download(
            @AuthenticationPrincipal(expression="id") Long userId,
            @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "false") boolean inline
    ) {
        Attachment a = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));
        // TODO: 권한검사 authz.checkCanReadSchedule(userId, a.getSchedule().getId());

        String url = storage.presignGet(a.getObjectKey(), a.getOriginalName(), inline);

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

