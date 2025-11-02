package com.calendarbox.backend.expense.repository;

import com.calendarbox.backend.expense.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findBySchedule_Id(Long id);
}
