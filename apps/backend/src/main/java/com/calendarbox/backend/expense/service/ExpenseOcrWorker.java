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

    /** 바깥 트랜잭션: 상태 전환 + task의 raw/normalized/expense 링크 저장 */
    @Transactional
    public int processBatch(int batchSize) {
        var tasks = expenseOcrTaskRepository.lockQueuedForProcess(batchSize);
        for (var t : tasks) {
            try {
                t.markRunning();
                expenseOcrTaskRepository.saveAndFlush(t);

                // 내부 트랜잭션: OCR호출, Expense/Line/Attachment 생성
                OcrProcessResult result = processOne(t);

                // 바깥에서 task에 결과 반영
                t.updateRawResponse(result.raw());
                t.updateNormalized(result.normalized());
                if (result.expense() != null) {
                    t.linkExpense(result.expense());
                }
                t.markSuccess();
            } catch (Exception e) {
                log.warn("[OCR] task={} failed: {}", t.getOcrTaskId(), e.getMessage());
                t.markFailed(e.getMessage());
            }
            expenseOcrTaskRepository.saveAndFlush(t);
        }
        return tasks.size();
    }

    /** 내부 트랜잭션: 순수 작업만 수행하고 결과 반환 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected OcrProcessResult processOne(ExpenseOcrTask task) {
        var att = task.getAttachment();

        // 1) S3 바이트 -> base64
        byte[] bytes = storageClient.getObjectBytes(att.getObjectKey());
        String b64 = Base64.getEncoder().encodeToString(bytes);

        // 2) 확장자/포맷
        String ext = extFrom(att.getOriginalName()); // jpg/png/pdf 체크 포함

        // 3) 네이버 OCR 바디 생성 (data: base64)
        Map<String, Object> body = buildOcrBodyV2(ext, b64);

        // 4) 요청
        Map<String, Object> raw = naverOcrClient.request(body);

        // 5) normalize (가변 map/list로 생성되는 asMap 사용)
        var norm = OcrNormalize.normalize(raw);
        Map<String, Object> normMap = norm.asMap();

        // 6) DB 저장 (Expense / ExpenseLine / ExpenseAttachment)
        Expense expense = expenseRepository.save(Expense.fromReceipt(
                task.getSchedule(),
                norm.getMerchantNameOrDefault("영수증"),
                norm.totalAmount(),
                norm.paidAt(),
                normMap
        ));

        var lines = norm.items().stream()
                .map(i -> ExpenseLine.of(expense, i.label(), i.qty(), i.unitAmount(), i.lineAmount()))
                .toList();
        expenseLineRepository.saveAll(lines);

        expenseAttachmentRepository.save(ExpenseAttachment.of(expense, att));

        return new OcrProcessResult(raw, normMap, expense);
    }

    /* ====================== helpers ====================== */

    private static String extFrom(String originalName) {
        String ext = Optional.ofNullable(originalName)
                .filter(n -> n.contains("."))
                .map(n -> n.substring(n.lastIndexOf('.') + 1))
                .map(String::toLowerCase)
                .orElse("jpg");
        if ("jpeg".equals(ext)) ext = "jpg";
        if (!List.of("jpg", "png", "pdf").contains(ext)) {
            throw new IllegalArgumentException("Unsupported image format: " + ext);
        }
        return ext;
    }

    private static Map<String, Object> buildOcrBodyV2(String ext, String base64data) {
        Map<String, Object> image = new LinkedHashMap<>();
        image.put("format", ext);
        image.put("name", "receipt");
        image.put("data", base64data);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", "V2");
        body.put("requestId", "req-" + System.currentTimeMillis());
        body.put("timestamp", System.currentTimeMillis());
        body.put("images", List.of(image));
        return body;
    }
}
