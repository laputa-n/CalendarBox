package com.calendarbox.backend.expense.repository;

import com.calendarbox.backend.expense.domain.ExpenseAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseAttachmentRepository extends JpaRepository<ExpenseAttachment, Long> {
}
