package com.calendarbox.backend.expense.repository;

import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.expense.domain.ExpenseOcrTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseOcrTaskRepository extends JpaRepository<ExpenseOcrTask, Long> {
    Optional<ExpenseOcrTask> findByRequestHash(String requestHash);

    @Query(
            value = """
        SELECT * FROM expense_ocr_task
        WHERE status = 'QUEUED'
        ORDER BY created_at
        FOR UPDATE SKIP LOCKED
        LIMIT :batch
      """,
            nativeQuery = true
    )
    List<ExpenseOcrTask> lockQueuedForProcess(@Param("batch") int batch);

    List<ExpenseOcrTask> findByAttachment(Attachment attachment);
}
