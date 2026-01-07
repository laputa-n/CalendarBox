package com.calendarbox.backend.schedule.util;

import com.calendarbox.backend.global.config.EmbeddingMqConfig;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmbeddingEnqueueService {
    private final ScheduleRepository scheduleRepository;
    private final RabbitTemplate rabbitTemplate;

    public void enqueueAfterCommit(Long scheduleId) {
        int updated = scheduleRepository.markEmbeddingQueued(scheduleId);
        if (updated == 0) return;

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            rabbitTemplate.convertAndSend(EmbeddingMqConfig.EXCHANGE, EmbeddingMqConfig.RK_RUN, scheduleId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                rabbitTemplate.convertAndSend(EmbeddingMqConfig.EXCHANGE, EmbeddingMqConfig.RK_RUN, scheduleId);
            }
        });
    }

    public void publishAfterCommit(List<Long> scheduleIds, String reason) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Long id : scheduleIds) {
                    rabbitTemplate.convertAndSend(EmbeddingMqConfig.EXCHANGE, EmbeddingMqConfig.RK_RUN, id);
                }
                // 로그는 여기서 한 번에
            }
        });
    }
}
