package com.calendarbox.backend.schedule.domain;

import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.schedule.enums.ScheduleTheme;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Schedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id")
    private Calendar calendar;

    @Column(nullable = false)
    private String title;

    private String memo;

    @Enumerated(EnumType.STRING)
    private ScheduleTheme theme;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private Member createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Member updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "source_schedule_id", nullable = true)
    private Schedule source;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<ScheduleLink> links = new ArrayList<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    private List<SchedulePlace> places = new ArrayList<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("invitedAt ASC, id ASC")
    private List<ScheduleParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderNo Asc, id ASC")
    private List<ScheduleTodo> todos = new ArrayList<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position Asc, id ASC")
    private List<Attachment> attachments = new ArrayList<>();

    public void addLink(ScheduleLink link) {
        links.add(link);
        link.setSchedule(this);
    }

    public void removeLink(ScheduleLink link) {
        links.remove(link);
        link.setSchedule(null);
    }

    public void addPlace(SchedulePlace schedulePlace) {
        places.add(schedulePlace);
        schedulePlace.setSchedule(this);
    }

    public void removePlace(SchedulePlace schedulePlace) {
        places.remove(schedulePlace);
        schedulePlace.setSchedule(null);
    }

    public void addTodo(ScheduleTodo todo) {
        todos.add(todo);
        todo.setSchedule(this);
    }

    public void removeTodo(ScheduleTodo todo) {
        todos.remove(todo);
        todo.setSchedule(null);
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
        attachment.setSchedule(this);
    }

    public void removeAttachment(Attachment attachment) {
        attachments.remove(attachment);
        attachment.setSchedule(null);
    }

    public void addParticipant(ScheduleParticipant sp) {
        participants.add(sp);
        sp.setSchedule(this);
    }
    public void removeParticipant(ScheduleParticipant sp) {
        participants.remove(sp);   // <-- orphanRemoval 트리거
        sp.setSchedule(null);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
    }
    @PreUpdate
    void preUpdate() { this.updatedAt = Instant.now(); }

    public static Schedule cloneHeader(
            Schedule src,
            Calendar targetCalendar,
            Member createdBy,
            Instant startAt,
            Instant endAt
    ) {
        Schedule dst = new Schedule();
        dst.calendar = targetCalendar;
        dst.title = src.title;
        dst.memo = src.memo;
        dst.theme = src.theme;
        dst.startAt = startAt;
        dst.endAt = endAt;
        dst.createdBy = createdBy;
        dst.source = src;
        return dst;
    }
}
