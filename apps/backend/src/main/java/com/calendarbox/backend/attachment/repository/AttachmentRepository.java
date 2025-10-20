package com.calendarbox.backend.attachment.repository;

import com.calendarbox.backend.attachment.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query(value = """
        INSERT INTO attachment
            (schedule_id, original_name, object_key, mime_type, byte_size, position, created_at, created_by, is_img)
        SELECT 
            :dstId, original_name, object_key, mime_type, byte_size, position, now(), created_by, is_img
        FROM attachment
        WHERE schedule_id = :srcId
        """, nativeQuery = true)
    void copyAllDbOnly(@Param("srcId") Long srcId, @Param("dstId") Long dstId);

    boolean existsBySchedule_IdAndIsImg(Long scheduleId, Boolean isImg);

    Long countBySchedule_IdAndIsImg(Long scheduleId, Boolean isImg);
}
