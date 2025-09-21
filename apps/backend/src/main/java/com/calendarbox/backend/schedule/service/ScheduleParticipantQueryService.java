package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.schedule.domain.ScheduleParticipant;
import com.calendarbox.backend.schedule.dto.response.ScheduleParticipantResponse;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleParticipantQueryService {
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    public List<ScheduleParticipantResponse> list(
            Long userId, ScheduleParticipantStatus status, Long scheduleId
    ){
        List<ScheduleParticipant> splist = scheduleParticipantRepository.findAllByScheduleAndStatus(scheduleId,status);

        return splist.stream().map(this::toResponse).toList();
    }
    private ScheduleParticipantResponse toResponse(ScheduleParticipant sp) {
        Long memberId = (sp.getMember() != null) ? sp.getMember().getId() : null;
        return new ScheduleParticipantResponse(
                sp.getId(),
                sp.getSchedule().getId(),
                memberId,
                sp.getName(),
                sp.getInvitedAt(),
                sp.getRespondedAt(),
                sp.getStatus()
        );
    }
}
