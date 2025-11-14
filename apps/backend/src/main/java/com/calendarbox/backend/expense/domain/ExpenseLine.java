package com.calendarbox.backend.expense.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

import static java.time.Instant.now;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class ExpenseLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_line_id")
    private Long id;

    @Setter(AccessLevel.PROTECTED)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_amount", nullable = false)
    private Long unitAmount = 0L;

    @Column(name = "line_amount", nullable = false)
    private Long lineAmount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ExpenseLine of(Expense expense, String label, Integer quantity, Long unitAmount, Long lineAmount) {
        ExpenseLine expenseLine = new ExpenseLine();
        expenseLine.expense = expense;
        expenseLine.label = label;
        expenseLine.quantity = (quantity == null || quantity <= 0) ? 1 : quantity;
        expenseLine.unitAmount = (unitAmount != null) ? unitAmount : lineAmount / expenseLine.quantity;
        expenseLine.lineAmount = lineAmount;
        return expenseLine;
    }

    public void changeLabel(String label) {
        this.label = label;
    }
    public void changeQuantity(Integer quantity) {
        this.quantity = (quantity == null || quantity <= 0) ? 1 : quantity;
    }
    public void changeUnitAmount(Long unitAmount) {
        this.unitAmount = (unitAmount == null) ? 0L : unitAmount;
    }
    public void changeLineAmount(Long lineAmount) {
        this.lineAmount = (lineAmount == null) ? 0L : lineAmount;
    }
}
