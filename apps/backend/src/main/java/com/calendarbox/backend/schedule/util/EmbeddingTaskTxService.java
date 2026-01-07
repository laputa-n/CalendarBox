package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmbeddingTaskTxService {
    private final ScheduleRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long scheduleId) {
        int claimed = repo.markEmbeddingRunningIfQueued(scheduleId);
        repo.flush();
        return claimed == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long scheduleId) {
        var s = repo.findById(scheduleId).orElseThrow();
        s.markEmbeddingSynced(Instant.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedQueuedOrFailed(Long scheduleId, Exception e, boolean willRetry) {
        var s = repo.findById(scheduleId).orElseThrow();
        String msg = e.getMessage();
        if (msg == null) msg = e.getClass().getSimpleName();
        if (msg.length() > 500) msg = msg.substring(0, 500);
        s.markEmbeddingFailed(msg, willRetry);
    }
}
