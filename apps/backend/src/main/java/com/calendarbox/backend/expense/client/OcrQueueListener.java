package com.calendarbox.backend.expense.client;

import com.calendarbox.backend.expense.service.ExpenseOcrWorker;
import com.calendarbox.backend.expense.service.OcrTaskTxService;
import com.calendarbox.backend.global.config.OcrMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrQueueListener {
    private final OcrTaskTxService ocrTaskTxService;
    private final ExpenseOcrWorker expenseOcrWorker;
    private final RabbitTemplate rabbitTemplate;
    private static final String HDR_RETRY = "x-retry";

    @RabbitListener(queues = OcrMqConfig.OCR_QUEUE)
    public void handle(Long taskId, Message amqpMsg) {
        int beforeRetry = getRetry(amqpMsg);
        log.info("[OCR-MQ] recv taskId={}, retry={}", taskId, beforeRetry);

        // 1) claim을 REQUIRES_NEW로 커밋해서 락 바로 풀기
        if (!ocrTaskTxService.claim(taskId)) {
            log.info("[OCR-MQ] skip taskId={} (not QUEUED)", taskId);
            return;
        }
        log.info("[OCR-MQ] claimed taskId={}", taskId);

        try {
            expenseOcrWorker.processOneByTaskId(taskId); // 내부에서 expense/lines/task 링크까지
            ocrTaskTxService.markSuccess(taskId);
            log.info("[OCR-MQ] success taskId={}", taskId);

        } catch (Exception e) {
            int retry = beforeRetry + 1;
            boolean willRetry = retry <= 3;

            // 실패 상태 기록 (REQUIRES_NEW로 커밋)
            ocrTaskTxService.markFailedQueuedOrFailed(taskId, e, willRetry);

            if (!willRetry) {
                rabbitTemplate.convertAndSend(OcrMqConfig.EXCHANGE, OcrMqConfig.RK_DLQ, taskId);
                return;
            }

            String rk = switch (retry) {
                case 1 -> OcrMqConfig.RK_RETRY_10S;
                case 2 -> OcrMqConfig.RK_RETRY_1M;
                case 3 -> OcrMqConfig.RK_RETRY_10M;
                default -> OcrMqConfig.RK_DLQ;
            };

            rabbitTemplate.convertAndSend(OcrMqConfig.EXCHANGE, rk, taskId, m -> {
                m.getMessageProperties().setHeader(HDR_RETRY, retry);
                return m;
            });
        }
    }

    private int getRetry(Message amqpMsg) {
        Object v = amqpMsg.getMessageProperties().getHeaders().get(HDR_RETRY);
        if (v == null) return 0;
        try { return Integer.parseInt(String.valueOf(v)); }
        catch (Exception ignore) { return 0; }
    }
}

