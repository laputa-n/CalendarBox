package com.calendarbox.backend.attachment.service;

import com.calendarbox.backend.attachment.support.UploadCache;
import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.attachment.dto.request.CompleteRequest;
import com.calendarbox.backend.attachment.dto.request.PresignRequest;
import com.calendarbox.backend.attachment.dto.response.AttachmentDto;
import com.calendarbox.backend.attachment.dto.response.PresignResponse;
import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.global.infra.storage.StorageClient;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private static final Set<String> ALLOW = Set.of(
            "image/jpeg","image/png","image/webp","image/heic","image/avif","application/pdf"
    );

    private final StorageClient storage;
    private final UploadCache cache;
    private final AttachmentRepository attRepo;
    private final ScheduleRepository schRepo; // 기존 것
    private final MemberRepository memRepo;   // 기존 것

    @Transactional(readOnly = true)
    public PresignResponse presign(Long userId, PresignRequest req) {
        // 권한 체크 (스텁) authz.checkCanEditSchedule(userId, req.scheduleId());
        var ct = req.contentType().toLowerCase();
        if (!ALLOW.contains(ct)) throw new IllegalArgumentException("Unsupported content type");
        if (req.size() <= 0 || req.size() > 50L*1024*1024) throw new IllegalArgumentException("Invalid file size");

        String ext = Optional.ofNullable(FilenameUtils.getExtension(req.filename()))
                .map(String::toLowerCase).orElse("");
        String key = "schedules/%d/%s.%s".formatted(req.scheduleId(), UUID.randomUUID(), ext);

        String url = storage.presignPut(key, ct, req.size());
        String uploadId = cache.put(new PresignRequest(req.scheduleId(), req.filename(), ct, req.size()));
        return new PresignResponse(uploadId, key, url);
    }

    @Transactional
    public AttachmentDto complete(Long userId, CompleteRequest req) {
        var ctx = cache.get(req.uploadId());
        if (ctx == null) throw new IllegalArgumentException("Invalid uploadId");

        storage.assertExists(req.objectKey()); // 실제 업로드 확인

        var schedule = schRepo.getReferenceById(ctx.scheduleId());
        var member = memRepo.getReferenceById(userId);
        int pos = attRepo.findMaxPosition(schedule.getId()) + 1;

        var saved = attRepo.save(Attachment.of(
                schedule, member, ctx.filename(), req.objectKey(), ctx.contentType(), ctx.size(), pos
        ));
        cache.remove(req.uploadId());
        return new AttachmentDto(saved.getId(), saved.getOriginalName(), saved.getMimeType(), saved.getByteSize(), saved.getPosition());
    }
}

