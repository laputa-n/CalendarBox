package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.repository.ScheduleEmbeddingRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleEmbeddingWorker {

    private final ScheduleRepository scheduleRepository;
    private final DefaultScheduleEmbeddingService embeddingService;
    private final ScheduleEmbeddingRepository embeddingRepository;

    @Transactional
    public void processOne(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow();
        float[] vector = embeddingService.embedScheduleEntity(schedule);
        embeddingRepository.upsertEmbedding(scheduleId, vector);
    }
}
