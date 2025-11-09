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
import org.springframework.transaction.annotation.Transactional;

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
                processOne(t);
                t.markSuccess();
            } catch (Exception e) {
                t.markFailed(e.getMessage());
            }
        }
        return tasks.size();
    }

    protected void processOne(ExpenseOcrTask task){
        var s3url = storageClient.presignGet(task.getAttachment().getObjectKey(), task.getAttachment().getOriginalName(), true);
        var raw = naverOcrClient.request(s3url);
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
                .map(i -> ExpenseLine.of(expense,i.label(),i.qty(),i.unitAmount(), i.lineAmount()))
                .toList();
        expenseLineRepository.saveAll(lines);

        expenseAttachmentRepository.save(ExpenseAttachment.of(expense, task.getAttachment()));

        task.linkExpense(expense);
    }
}
