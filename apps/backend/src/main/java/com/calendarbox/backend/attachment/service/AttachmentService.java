package com.calendarbox.backend.attachment.service;

import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.attachment.dto.response.FileAttachmentDto;
import com.calendarbox.backend.attachment.dto.response.ImageAttachmentDto;
import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.calendar.domain.CalendarHistory;
import com.calendarbox.backend.calendar.enums.CalendarHistoryType;
import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarHistoryRepository;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.global.infra.storage.StorageClient;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class AttachmentService {

    private final ScheduleRepository scheduleRepository;
    private final AttachmentRepository attachmentRepository;
    private final StorageClient storage;
    private final MemberRepository memberRepository;
    private final CalendarHistoryRepository calendarHistoryRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final CalendarMemberRepository calendarMemberRepository;

    public List<ImageAttachmentDto> getImages(Long userId, Long scheduleId){
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        Calendar calendar = schedule.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),userId, CalendarMemberStatus.ACCEPTED)
                &&!scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(schedule.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        return attachmentRepository.findImagesByScheduleId(scheduleId).stream().map(a -> {
            String imageUrl = storage.presignGet(a.getObjectKey(), a.getOriginalName(), true);
            String thumbUrl = imageUrl;
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
    }
    public List<FileAttachmentDto> getFiles(Long userId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        Calendar calendar = schedule.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),userId, CalendarMemberStatus.ACCEPTED)
                &&!scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(schedule.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        return attachmentRepository.findFilesByScheduleId(scheduleId).stream().map(a ->
                new FileAttachmentDto(
                        a.getId(),
                        a.getOriginalName(),
                        a.getMimeType(),
                        a.getByteSize(),
                        storage.presignGet(a.getObjectKey(), a.getOriginalName(), false)
                )
        ).toList();
    }
    public void deleteAttachment(Long userId, Long attachmentId) {
        Attachment a = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule schedule = a.getSchedule();
        Calendar calendar = schedule.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),userId, CalendarMemberStatus.ACCEPTED)
                &&!scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(schedule.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        String originalKey = a.getObjectKey();
        String attachmentName = a.getOriginalName();
        String thumbKey = storage.toThumbKey(originalKey);

        try {
            storage.deleteQuietly(originalKey);
            storage.deleteQuietly(thumbKey);
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCode.ATTACHMENT_STORAGE_DELETE_FAIL);
        }

        schedule.removeAttachment(a);

        CalendarHistory history = CalendarHistory.builder()
                .calendar(calendar)
                .actor(user)
                .entityId(schedule.getId())
                .type(CalendarHistoryType.SCHEDULE_ATTACHMENT_REMOVED)
                .changedFields(Map.of("attachmentName",attachmentName))
                .build();
        calendarHistoryRepository.save(history);
    }

    public String getDownloadUrl(Long userId, Long attachmentId, boolean inline){
        Attachment a = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTACHMENT_NOT_FOUND));

        Schedule schedule = a.getSchedule();
        Calendar calendar = schedule.getCalendar();
        if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendar.getId(),userId, CalendarMemberStatus.ACCEPTED)
                &&!scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(schedule.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        return storage.presignGet(a.getObjectKey(), a.getOriginalName(), inline);
    }
}
