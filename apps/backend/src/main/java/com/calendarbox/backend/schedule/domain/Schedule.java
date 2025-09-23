package com.calendarbox.backend.schedule.domain;

import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.calendar.domain.Calendar;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.schedule.enums.ScheduleTheme;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.calendarbox.backend.schedule.enums.ScheduleTheme.BLACK;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Schedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "calendar_id", nullable = false)
    private Calendar calendar;

    @Column(nullable = false)
    private String title;

    private String memo;

    @Enumerated(EnumType.STRING)
    private ScheduleTheme theme = BLACK;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by")
    private Member updatedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
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


    public Schedule(Calendar c, String title, String memo, ScheduleTheme theme, Instant startAt, Instant endAt, Member createdBy) {
        this.calendar = c;
        this.title = title;
        this.memo = memo;
        this.theme = theme;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdBy = createdBy;
    }

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

    public void editTitle(String title){
        String t = (title == null)?"":title;
        this.title = t;
    }

    public void editMemo(String memo){
        this.memo = (memo == null ? "":memo);
    }

    public void editTheme(ScheduleTheme theme){
        this.theme = (theme == null? BLACK : theme);
    }

    public void reschedule(Instant startAt, Instant endAt) {
        this.startAt = startAt;
        this.endAt   = endAt;
    }

    public void touchUpdateBy(Member updater){
        this.updatedBy = updater;
    }
}
