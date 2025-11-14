package com.calendarbox.backend.expense.domain;

import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.expense.enums.OcrTaskStatus;
import com.calendarbox.backend.schedule.domain.Schedule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.SQLType;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ExpenseOcrTask {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ocr_task_id", nullable = false)
    private Long ocrTaskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attachment_id", nullable = false)
    private Attachment attachment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OcrTaskStatus status = OcrTaskStatus.QUEUED;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private Map<String,Object> rawResponse = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String,Object> normalized = Map.of();

    @Column(name = "request_hash", unique = true, nullable = false, updatable = false, length = 64)
    private String requestHash;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ExpenseOcrTask of(Attachment attachment, Schedule schedule, String requestHash) {
        ExpenseOcrTask task = new ExpenseOcrTask();
        task.attachment = attachment;
        task.schedule = schedule;
        task.status = OcrTaskStatus.QUEUED;
        task.requestHash = requestHash;

        return task;
    }

    public void markRunning() { this.status = OcrTaskStatus.RUNNING; }
    public void markSuccess() { this.status = OcrTaskStatus.SUCCESS; }
    // ExpenseOcrTask.java
    public void markFailed(Throwable t) {
        this.status = OcrTaskStatus.FAILED;
        if (t == null) { this.errorMessage = null; return; }
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getSimpleName()).append(": ")
                .append(Objects.toString(t.getMessage(), ""));
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append("\n  at ").append(el);
            if (sb.length() > 2000) break; // 과도한 길이 방지
        }
        this.errorMessage = sb.toString();
    }

    public void linkExpense(Expense e) { this.expense = e; }
    public void updateRawResponse(Map<String,Object> raw) { this.rawResponse = raw; }
    public void updateNormalized(Map<String,Object> norm) { this.normalized = norm; }
}
