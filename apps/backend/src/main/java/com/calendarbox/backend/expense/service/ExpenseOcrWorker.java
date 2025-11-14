package com.calendarbox.backend.expense.service;

import com.calendarbox.backend.expense.client.NaverOcrClient;
import com.calendarbox.backend.expense.domain.Expense;
import com.calendarbox.backend.expense.domain.ExpenseAttachment;
import com.calendarbox.backend.expense.domain.ExpenseLine;
import com.calendarbox.backend.expense.domain.ExpenseOcrTask;
import com.calendarbox.backend.expense.dto.response.OcrProcessResult;
import com.calendarbox.backend.expense.repository.ExpenseAttachmentRepository;
import com.calendarbox.backend.expense.repository.ExpenseLineRepository;
import com.calendarbox.backend.expense.repository.ExpenseOcrTaskRepository;
import com.calendarbox.backend.expense.repository.ExpenseRepository;
import com.calendarbox.backend.expense.support.OcrNormalize;
import com.calendarbox.backend.global.infra.storage.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseOcrWorker {
    private final ExpenseRepository expenseRepository;
    private final ExpenseAttachmentRepository expenseAttachmentRepository;
    private final ExpenseLineRepository expenseLineRepository;
    private final ExpenseOcrTaskRepository expenseOcrTaskRepository;
    private final NaverOcrClient naverOcrClient;
    private final StorageClient storageClient;

    @Transactional
    public int processBatch(int batchSize){
        var tasks = expenseOcrTaskRepository.lockQueuedForProcess(batchSize);
        for (var t : tasks) {
            try {
                t.markRunning();
                expenseOcrTaskRepository.save(t);
                processOne(t);
                t.markSuccess();
            } catch (Exception e) {
                log.error("[OCR] Task {} failed", t.getOcrTaskId(), e);
                t.markFailed(e);               // ← 메시지 null 방지
            }
            expenseOcrTaskRepository.save(t);
        }
        return tasks.size();
    }



    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processOne(ExpenseOcrTask task){
        log.info("[OCR] start taskId={}, attKey={}", task.getOcrTaskId(), task.getAttachment().getObjectKey());

        var att = task.getAttachment();

        // 1) S3 바이트 읽기
        byte[] bytes = storageClient.getObjectBytes(att.getObjectKey());
        log.info("[OCR] read s3 bytes: {}", (bytes == null ? 0 : bytes.length));

        // 2) 확장자/포맷
        String ext = Optional.ofNullable(att.getOriginalName())
                .map(n -> n.substring(n.lastIndexOf('.')+1).toLowerCase())
                .orElse("jpg");
        if (ext.equals("jpeg")) ext = "jpg";
        if (!List.of("jpg","png","pdf").contains(ext)) {
            throw new IllegalArgumentException("Unsupported image format: " + ext);
        }
        log.info("[OCR] ext={}", ext);

        // 3) base64 변환 + 요청 바디
        String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
        Map<String, Object> body = Map.of(
                "version", "V2",
                "requestId", "req-" + System.currentTimeMillis(),
                "timestamp", System.currentTimeMillis(),
                "images", List.of(Map.of(
                        "format", ext,
                        "name", "receipt",
                        "data", b64
                ))
        );
        log.info("[OCR] ready to call Naver OCR (len={})", b64.length());

        // 4) 호출
        Map<String,Object> raw = naverOcrClient.request(body);
        log.info("[OCR] got response keys={}", raw.keySet());
        task.updateRawResponse(raw);

        // 5) 정규화
        var norm = OcrNormalize.normalize(raw);
        task.updateNormalized(norm.asMap());
        log.info("[OCR] normalized total={}, items={}", norm.totalAmount(), norm.items().size());

        // 6) 저장
        var expense = expenseRepository.save(Expense.fromReceipt(
                task.getSchedule(),
                norm.getMerchantNameOrDefault("영수증"),
                norm.totalAmount(),
                norm.paidAt(),
                norm.asMap()
        ));
        var lines = norm.items().stream()
                .map(i -> ExpenseLine.of(expense, i.label(), i.qty(), i.unitAmount(), i.lineAmount()))
                .toList();
        expenseLineRepository.saveAll(lines);
        expenseAttachmentRepository.save(ExpenseAttachment.of(expense, att));
        task.linkExpense(expense);

        log.info("[OCR] success taskId={}, expenseId={}", task.getOcrTaskId(), expense.getId());
    }
}
