package com.calendarbox.backend.expense.service;

import com.calendarbox.backend.expense.repository.ExpenseOcrTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OcrTaskTxService {
    private final ExpenseOcrTaskRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long taskId) {
        int claimed = repo.markRunningIfQueued(taskId);
        repo.flush();
        return claimed == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long taskId) {
        var task = repo.findById(taskId).orElseThrow();
        task.markSuccess();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedQueuedOrFailed(Long taskId, Exception e, boolean willRetry) {
        var task = repo.findById(taskId).orElseThrow();
        task.markFailed(e);
        if (willRetry) task.markQueued();
    }
}
