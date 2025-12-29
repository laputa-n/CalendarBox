package com.calendarbox.backend.expense.client;

import com.calendarbox.backend.expense.repository.ExpenseOcrTaskRepository;
import com.calendarbox.backend.expense.service.ExpenseOcrWorker;
import com.calendarbox.backend.global.config.OcrMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrQueueListener {
    private final ExpenseOcrTaskRepository expenseOcrTaskRepository;
    private final ExpenseOcrWorker expenseOcrWorker;
    private final RabbitTemplate rabbitTemplate;
    private static final String HDR_RETRY = "x-retry";

    @RabbitListener(queues = OcrMqConfig.OCR_QUEUE)
    @Transactional
    public void handle(Long taskId, Message amqpMsg) {
        int beforeRetry = getRetry(amqpMsg);
        log.info("[OCR-MQ] recv taskId={}, retry={}", taskId, beforeRetry);

        int claimed = expenseOcrTaskRepository.markRunningIfQueued(taskId);
        if (claimed == 0) {
            log.info("[OCR-MQ] skip taskId={} (not QUEUED)", taskId);
            return;
        }
        log.info("[OCR-MQ] claimed taskId={}", taskId);

        try {
            log.info("[OCR-MQ] start worker taskId={}", taskId);
            expenseOcrWorker.processOneByTaskId(taskId);

            var task = expenseOcrTaskRepository.findById(taskId).orElseThrow();
            task.markSuccess();
            expenseOcrTaskRepository.save(task);
            log.info("[OCR-MQ] success taskId={}", taskId);

        } catch (Exception e) {
            var task = expenseOcrTaskRepository.findById(taskId).orElseThrow();

            boolean s3Missing = (e instanceof IllegalStateException)
                    && e.getMessage() != null
                    && e.getMessage().contains("S3 object not found");

            if (s3Missing) {
                task.markFailed(e);
                expenseOcrTaskRepository.save(task);
                rabbitTemplate.convertAndSend(OcrMqConfig.EXCHANGE, OcrMqConfig.RK_DLQ, taskId);
                log.warn("[OCR-MQ] permanent fail (missing S3) taskId={}", taskId);
                return;
            }
            int retry = beforeRetry + 1;
            boolean willRetry = retry <= 3;

            log.error("[OCR-MQ] failed taskId={}, retry={}", taskId, retry, e);

            task.markFailed(e);
            if (willRetry) task.markQueued();
            expenseOcrTaskRepository.save(task);

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
