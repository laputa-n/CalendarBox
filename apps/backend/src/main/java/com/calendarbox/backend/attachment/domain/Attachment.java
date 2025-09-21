package com.calendarbox.backend.attachment.domain;

import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.schedule.domain.Schedule;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Attachment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "byte_size", nullable = false)
    private Long byteSize;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;


    @Column(name = "is_img", nullable = false)
    private Boolean isImg;


    public static Attachment of(Schedule schedule, Member createdBy,
                                String originalName, String objectKey,
                                String mimeType, long byteSize, int position) {
        Attachment a = new Attachment();
        a.schedule = schedule;
        a.createdBy = createdBy;
        a.originalName = originalName;
        a.objectKey = objectKey;
        a.mimeType = (mimeType == null) ? null : mimeType.toLowerCase();
        a.byteSize = byteSize;
        a.position = position;
        a.isImg = a.mimeType != null && a.mimeType.startsWith("image/");
        a.createdAt = Instant.now();
        return a;
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.mimeType != null) this.mimeType = this.mimeType.toLowerCase();
        this.isImg = this.mimeType != null && this.mimeType.startsWith("image/");
    }
    public void setSchedule(Schedule schedule){ this.schedule = schedule; }
}
