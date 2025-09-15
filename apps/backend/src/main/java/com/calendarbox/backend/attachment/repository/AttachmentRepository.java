package com.calendarbox.backend.attachment.repository;

import com.calendarbox.backend.attachment.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    @Query("""
            select a from Attachment a 
            where a.schedule.id = :scheduleId and a.isImg = true
            order by a.position asc, a.id asc
            """)
    List<Attachment> findImagesByScheduleId(Long scheduleId);

    @Query("""
            select a from Attachment a 
            where a.schedule.id = :scheduleId and a.isImg = false
            order by a.createdAt asc, a.id asc
            """)
    List<Attachment> findFilesByScheduleId(Long scheduleId);

    @Query("select coalesce(max(a.position), -1) from Attachment a where a.schedule.id = :scheduleId")
    int findMaxPosition(Long scheduleId);
}
