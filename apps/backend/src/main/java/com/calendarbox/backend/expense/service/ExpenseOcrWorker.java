package com.calendarbox.backend.expense.service;

import com.calendarbox.backend.expense.client.NaverOcrClient;
import com.calendarbox.backend.expense.domain.Expense;
import com.calendarbox.backend.expense.domain.ExpenseAttachment;
import com.calendarbox.backend.expense.domain.ExpenseLine;
import com.calendarbox.backend.expense.domain.ExpenseOcrTask;
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

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        for(var t: tasks){
            try{
                t.markRunning();
                expenseOcrTaskRepository.save(t);
                processOne(t);
                t.markSuccess();
            } catch (Exception e) {
                t.markFailed(e.getMessage());
            }
            expenseOcrTaskRepository.save(t);
        }
        return tasks.size();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processOne(ExpenseOcrTask task){
        var att = task.getAttachment();

        // 1) S3에서 바이트 읽기 -> base64
        byte[] bytes = storageClient.getObjectBytes(att.getObjectKey());
        String b64 = Base64.getEncoder().encodeToString(bytes);

        // 2) 확장자/포맷 일치
        String ext = Optional.ofNullable(att.getOriginalName())
                .map(n -> n.substring(n.lastIndexOf('.')+1).toLowerCase())
                .orElse("jpg");
        if (ext.equals("jpeg")) ext = "jpg";
        if (!List.of("jpg","png","pdf").contains(ext)) {
            throw new IllegalArgumentException("Unsupported image format: " + ext);
        }

        // 3) 네이버 OCR 바디 (base64 data 사용)
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

        // 4) 요청 + 에러 바디까지 기록
        Map<String,Object> raw = naverOcrClient.request(body);
        task.updateRawResponse(raw);

        var norm = OcrNormalize.normalize(raw);
        task.updateNormalized(norm.asMap());

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
    }
}
