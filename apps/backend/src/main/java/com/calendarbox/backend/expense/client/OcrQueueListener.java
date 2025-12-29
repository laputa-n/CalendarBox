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
        int claimed = expenseOcrTaskRepository.markRunningIfQueued(taskId);

        if (claimed == 0) {
            log.info("[OCR-MQ] skip taskId={} (not QUEUED)", taskId);
            return;
        }

        log.info("[OCR-MQ] claimed taskId={} (QUEUED -> RUNNING)", taskId);

        try {
            expenseOcrWorker.processOneByTaskId(taskId);

            // 성공 처리
            var task = expenseOcrTaskRepository.findById(taskId).orElseThrow();
            task.markSuccess();
            expenseOcrTaskRepository.save(task);
            log.info("[OCR-MQ] success taskId={}", taskId);

        } catch (Exception e) {
            int retry = getRetry(amqpMsg) + 1; // 첫 실패면 1

            boolean willRetry = retry <= 3; // 재시도 3회까지만(총 4번)

            var task = expenseOcrTaskRepository.findById(taskId).orElseThrow();
            task.markFailed(e);

            if (willRetry) {
                // 재시도 예정이면 QUEUED로 되돌려야 다음 메시지에서 다시 잡힘
                task.markQueued();
            }
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
