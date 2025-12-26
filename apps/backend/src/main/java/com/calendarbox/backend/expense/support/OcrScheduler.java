package com.calendarbox.backend.expense.support;

import com.calendarbox.backend.expense.service.ExpenseOcrWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OcrScheduler {
    private final ExpenseOcrWorker expenseOcrWorker;

    //@Scheduled(fixedDelay = 30000) // 5초 간격 추천
    //@Async
    public void tick() {
        expenseOcrWorker.processBatch(10);
    }
}
