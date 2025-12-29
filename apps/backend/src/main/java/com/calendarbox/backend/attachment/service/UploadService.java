package com.calendarbox.backend.attachment.service;

import com.calendarbox.backend.expense.domain.ExpenseOcrTask;
import com.calendarbox.backend.attachment.support.UploadCache;
import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.attachment.dto.request.CompleteRequest;
import com.calendarbox.backend.attachment.dto.request.PresignRequest;
import com.calendarbox.backend.attachment.dto.response.AttachmentDto;
import com.calendarbox.backend.attachment.dto.response.PresignResponse;
import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.calendar.repository.CalendarRepository;
import com.calendarbox.backend.expense.enums.OcrTaskStatus;
import com.calendarbox.backend.expense.repository.ExpenseOcrTaskRepository;
import com.calendarbox.backend.global.config.OcrMqConfig;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.global.infra.storage.StorageClient;
import com.calendarbox.backend.global.utils.HashUtil;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {
    private static final Set<String> ALLOW = Set.of(
            // 이미지
            "image/jpeg","image/png","image/webp","image/heic","image/avif","image/gif",
            // 문서
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            // 한글
            "application/x-hwp"
    );
    private final CalendarMemberRepository calendarMemberRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final StorageClient storage;
    private final UploadCache cache;
    private final AttachmentRepository attachmentRepository;
    private final ScheduleRepository scheduleRepository; // 기존 것
    private final MemberRepository memberRepository;   // 기존 것
    private final CalendarRepository calendarRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;
    private final ExpenseOcrTaskRepository expenseOcrTaskRepository;
    private final RabbitTemplate rabbitTemplate;
    @Transactional(readOnly = true)
    public PresignResponse presign(Long userId, PresignRequest req) {
        Member user = memberRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(req.scheduleId()).orElseThrow(()->new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        Calendar calendar = schedule.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),user.getId(), CalendarMemberStatus.ACCEPTED)
        &&!scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(schedule.getId(),userId, ScheduleParticipantStatus.ACCEPTED)
        &&!schedule.getCreatedBy().getId().equals(user.getId()))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        var ct = req.contentType().toLowerCase();
        if (!ALLOW.contains(ct)) throw new BusinessException(ErrorCode.ATTACHMENT_TYPE_NOT_SUPPORTED);
        if (req.size() <= 0 || req.size() > 50L*1024*1024) throw new BusinessException(ErrorCode.ATTACHMENT_SIZE_NOT_INVALID);

        String ext = Optional.ofNullable(FilenameUtils.getExtension(req.filename()))
                .map(String::toLowerCase).orElse("");
        String folder = req.isReceipt()?"receipts":"attachments";
        String key = "schedules/%d/%s/%s.%s".formatted(req.scheduleId(),folder, UUID.randomUUID(), ext);

        String url = storage.presignPut(key, ct, req.size());
        String uploadId = cache.put(new PresignRequest(req.scheduleId(), req.filename(), ct, req.size(),req.isReceipt()));
        return new PresignResponse(uploadId, key, url);
    }

    @Transactional
    public AttachmentDto complete(Long userId, CompleteRequest req) {
        var ctx = cache.get(req.uploadId());
        if (ctx == null) throw new BusinessException(ErrorCode.ATTACHMENT_NOT_CACHED);
        storage.assertExists(req.objectKey()); // 실제 업로드 확인

        var schedule = scheduleRepository.getReferenceById(ctx.scheduleId());
        var member = memberRepository.getReferenceById(userId);
        var calendar = calendarRepository.getReferenceById(schedule.getCalendar().getId());
        int pos = attachmentRepository.findMaxPosition(schedule.getId()) + 1;

        var saved = attachmentRepository.save(Attachment.of(
                schedule, member, ctx.filename(), req.objectKey(), ctx.contentType(), ctx.size(), pos
        ));
        cache.remove(req.uploadId());
        if(ctx.isReceipt() || saved.getObjectKey().contains("/receipts/")){
            ExpenseOcrTask task = expenseOcrTaskRepository.save(
                    ExpenseOcrTask.of(saved, schedule, HashUtil.sha256(saved.getObjectKey()))
            );
            rabbitTemplate.convertAndSend(OcrMqConfig.EXCHANGE, OcrMqConfig.RK_RUN, task.getOcrTaskId());
        }
        CalendarHistory history = CalendarHistory.builder()
                .calendar(calendar)
                .actor(member)
                .entityId(schedule.getId())
                .type(CalendarHistoryType.SCHEDULE_ATTACHMENT_ADDED)
                .changedFields(Map.of("attachmentName",saved.getOriginalName()))
                .build();
        calendarHistoryRepository.save(history);

        return new AttachmentDto(saved.getId(), saved.getOriginalName(), saved.getMimeType(), saved.getByteSize(), saved.getPosition());
    }
}

