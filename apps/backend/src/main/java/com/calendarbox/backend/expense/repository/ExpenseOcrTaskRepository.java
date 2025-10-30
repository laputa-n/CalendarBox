package com.calendarbox.backend.expense.repository;

import com.calendarbox.backend.expense.domain.ExpenseOcrTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseOcrTaskRepository extends JpaRepository<ExpenseOcrTask, Integer> {
    Optional<ExpenseOcrTask> findByRequestHash(String requestHash);
}
