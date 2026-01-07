package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.global.config.EmbeddingMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingQueueListener {

    private final EmbeddingTaskTxService txService;
    private final ScheduleEmbeddingWorker worker;
    private final RabbitTemplate rabbitTemplate;

    private static final String HDR_RETRY = "x-retry";

    @RabbitListener(queues = EmbeddingMqConfig.EMBEDDING_QUEUE)
    public void handle(Long scheduleId, Message amqpMsg) {
        int beforeRetry = getRetry(amqpMsg);
        log.info("[EMBED-MQ] recv scheduleId={}, retry={}", scheduleId, beforeRetry);

        if (!txService.claim(scheduleId)) {
            log.info("[EMBED-MQ] skip scheduleId={} (not QUEUED/dirty)", scheduleId);
            return;
        }

        try {
            worker.processOne(scheduleId);
            txService.markSuccess(scheduleId);
            log.info("[EMBED-MQ] success scheduleId={}", scheduleId);

        } catch (Exception e) {
            int retry = beforeRetry + 1;
            boolean willRetry = retry <= 3;

            txService.markFailedQueuedOrFailed(scheduleId, e, willRetry);

            if (!willRetry) {
                rabbitTemplate.convertAndSend(EmbeddingMqConfig.EXCHANGE, EmbeddingMqConfig.RK_DLQ, scheduleId);
                return;
            }

            String rk = switch (retry) {
                case 1 -> EmbeddingMqConfig.RK_RETRY_10S;
                case 2 -> EmbeddingMqConfig.RK_RETRY_1M;
                case 3 -> EmbeddingMqConfig.RK_RETRY_10M;
                default -> EmbeddingMqConfig.RK_DLQ;
            };

            rabbitTemplate.convertAndSend(EmbeddingMqConfig.EXCHANGE, rk, scheduleId, m -> {
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
