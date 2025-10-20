package com.calendarbox.backend.attachment.controller;

import com.calendarbox.backend.attachment.service.UploadService;
import com.calendarbox.backend.attachment.dto.request.CompleteRequest;
import com.calendarbox.backend.attachment.dto.request.PresignRequest;
import com.calendarbox.backend.attachment.dto.response.AttachmentDto;
import com.calendarbox.backend.attachment.dto.response.PresignResponse;
import com.calendarbox.backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attachments/uploads")
public class UploadController {
    private final UploadService svc;
    public UploadController(UploadService svc){ this.svc = svc; }

    @PostMapping("/presign")
    public ResponseEntity<ApiResponse<PresignResponse>> presign(
            @AuthenticationPrincipal(expression="id") Long userId,
            @RequestBody PresignRequest req) {
        var data = svc.presign(userId, req);
        return ResponseEntity.ok(ApiResponse.ok("presign 성공",data));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<AttachmentDto>> complete(
            @AuthenticationPrincipal(expression="id") Long userId,
            @RequestBody CompleteRequest req) {
        var data = svc.complete(userId, req);
        return ResponseEntity.ok(ApiResponse.ok("업로드 성공",data));
    }
}
