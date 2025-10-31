package com.calendarbox.backend.expense.repository;

import com.calendarbox.backend.expense.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}
