package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.ScheduleRecurrence;
import com.calendarbox.backend.schedule.dto.response.RecurrenceResponse;
import com.calendarbox.backend.schedule.repository.ScheduleRecurrenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleRecurrenceQueryService {
    private final ScheduleRecurrenceRepository scheduleRecurrenceRepository;

    public List<RecurrenceResponse> list(Long userId, Long scheduleId) {
        // 권한 체크 생략
        return scheduleRecurrenceRepository.findAllBySchedule_Id(scheduleId).stream()
                .map(this::toResponse).toList();
    }

    public RecurrenceResponse getOne(Long userId, Long scheduleId, Long recurrenceId) {
        var r = scheduleRecurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECURRENCE_NOT_FOUND));
        if (!r.getSchedule().getId().equals(scheduleId))
            throw new BusinessException(ErrorCode.SCHEDULE_RECUR_EXDATE_MISMATCH);
        return toResponse(r);
    }

    private RecurrenceResponse  toResponse(ScheduleRecurrence r) {
        return new RecurrenceResponse(
                r.getId(), r.getFreq(), r.getIntervalCount(),
                r.getByDay()==null ? List.of() : Arrays.asList(r.getByDay()),
                r.getByMonthday()==null ? List.of() : Arrays.asList(r.getByMonthday()),
                r.getByMonth()==null ? List.of() : Arrays.asList(r.getByMonth()),
                r.getUntil(), r.getCreatedAt()
        );
    }
}

