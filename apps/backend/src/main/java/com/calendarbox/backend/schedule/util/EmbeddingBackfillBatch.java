package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.global.config.EmbeddingMqConfig;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingBackfillBatch {

    private final ScheduleRepository scheduleRepository;
    private final RabbitTemplate rabbitTemplate;

    // 처음엔 200~500 정도로 작게
    private static final int BATCH_SIZE = 200;

    // 매일 새벽 3:30 (한국 기준). 원하는 시간으로 변경 가능.
    @Scheduled(cron = "0 50 13 * * *", zone = "Asia/Seoul")
    @Transactional // readOnly 제거!
    public void runDaily() {
        List<Long> ids = scheduleRepository.findScheduleIdsWithoutEmbedding(BATCH_SIZE);
        if (ids.isEmpty()) return;

        int queued = scheduleRepository.markEmbeddingQueuedForBackfill(ids);
        if (queued == 0) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Long scheduleId : ids) {
                    rabbitTemplate.convertAndSend(EmbeddingMqConfig.EXCHANGE, EmbeddingMqConfig.RK_RUN, scheduleId);
                }
                log.info("[EMBED-BACKFILL] selected={}, queued={}, published={}", ids.size(), queued, ids.size());
            }
        });
    }
}
