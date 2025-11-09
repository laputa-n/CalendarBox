package com.calendarbox.backend.expense.domain;

import com.calendarbox.backend.attachment.domain.Attachment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ExpenseAttachment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_attachment_id")
    private Long id;

    @Setter(AccessLevel.PROTECTED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Setter(AccessLevel.PUBLIC)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attachment_id", nullable = false)
    private Attachment attachment;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ExpenseAttachment of(Expense expense, Attachment attachment) {
        ExpenseAttachment expenseAttachment = new ExpenseAttachment();
        expenseAttachment.expense = expense;
        expenseAttachment.attachment = attachment;
        return expenseAttachment;
    }
}
