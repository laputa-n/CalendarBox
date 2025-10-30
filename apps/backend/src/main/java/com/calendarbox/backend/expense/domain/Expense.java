package com.calendarbox.backend.expense.domain;

import com.calendarbox.backend.expense.enums.ExpenseSource;
import com.calendarbox.backend.expense.enums.ReceiptParseStatus;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseSource source = ExpenseSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_parse_status", length = 15)
    private ReceiptParseStatus receiptParseStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_payload", columnDefinition = "jsonb")
    private Map parsedPayload = Map.of();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseAttachment> attachments = new ArrayList<>();

    public void addLine(ExpenseLine line) {
        lines.add(line);
        line.setExpense(this);
    }

    public void removeLine(ExpenseLine line){
        lines.remove(line);
        line.setExpense(null);
    }

    public void addAttachment(ExpenseAttachment attachment) {
        attachments.add(attachment);
        attachment.setExpense(this);
    }

    public void removeAttachment(ExpenseAttachment attachment){
        attachments.remove(attachment);
        attachment.setExpense(null);
    }

    public static Expense fromReceipt(Schedule schedule, String name, Long amount, Instant paidAt, Map parsedPayload) {
        Expense expense = new Expense();
        expense.schedule = schedule;
        expense.name = name;
        expense.amount = amount;
        expense.paidAt = paidAt;
        expense.source = ExpenseSource.RECEIPT;
        expense.receiptParseStatus = ReceiptParseStatus.SUCCESS;
        expense.parsedPayload = parsedPayload;
        return expense;
    }
}
