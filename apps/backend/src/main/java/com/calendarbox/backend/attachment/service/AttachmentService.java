package com.calendarbox.backend.attachment.service;

import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.global.infra.storage.StorageClient;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final StorageClient storage;
    private final MemberRepository memberRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;

    public void deleteAttachment(Long userId, Long attachmentId) {
        Attachment a = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule s = a.getSchedule();
        Calendar c = s.getCalendar();
        String originalKey = a.getObjectKey();
        String attachmentName = a.getOriginalName();
        String thumbKey = storage.toThumbKey(originalKey);

        try {
            storage.deleteQuietly(originalKey);
            storage.deleteQuietly(thumbKey);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Storage delete failed. Try again later.", ex);
        }

        attachmentRepository.delete(a);

        CalendarHistory history = CalendarHistory.builder()
                .calendar(c)
                .actor(user)
                .entityId(s.getId())
                .type(CalendarHistoryType.SCHEDULE_ATTACHMENT_REMOVED)
                .changedFields(Map.of("attachmentName",attachmentName))
                .build();
        calendarHistoryRepository.save(history);
    }
}
