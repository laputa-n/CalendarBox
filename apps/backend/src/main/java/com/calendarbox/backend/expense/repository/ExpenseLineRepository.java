package com.calendarbox.backend.expense.repository;

import com.calendarbox.backend.expense.domain.ExpenseLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseLineRepository extends JpaRepository<ExpenseLine, Long> {
}
