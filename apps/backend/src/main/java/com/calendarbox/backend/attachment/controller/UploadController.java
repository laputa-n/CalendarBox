package com.calendarbox.backend.attachment.controller;

import com.calendarbox.backend.attachment.service.UploadService;
import com.calendarbox.backend.attachment.dto.request.CompleteRequest;
import com.calendarbox.backend.attachment.dto.request.PresignRequest;
import com.calendarbox.backend.attachment.dto.response.AttachmentDto;
import com.calendarbox.backend.attachment.dto.response.PresignResponse;
import com.calendarbox.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Schedule - Attachment", description = "스케줄 첨부 파일")
@RestController
@RequestMapping("/api/attachments/uploads")
@RequiredArgsConstructor
public class UploadController {
    private final UploadService svc;

    @Operation(
            summary = "presign 발급",
            description = "첨부 파일 업로드를 위한 presign을 발급받습니다."
    )
    @PostMapping("/presign")
    public ResponseEntity<ApiResponse<PresignResponse>> presign(
            @AuthenticationPrincipal(expression="id") Long userId,
            @RequestBody PresignRequest req) {
        var data = svc.presign(userId, req);
        return ResponseEntity.ok(ApiResponse.ok("presign 성공",data));
    }

    @Operation(
            summary = "첨부 파일 업로드",
            description = "첨부 파일을 업로드 합니다."
    )
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<AttachmentDto>> complete(
            @AuthenticationPrincipal(expression="id") Long userId,
            @RequestBody CompleteRequest req) {
        var data = svc.complete(userId, req);
        return ResponseEntity.ok(ApiResponse.ok("업로드 성공",data));
    }
}
