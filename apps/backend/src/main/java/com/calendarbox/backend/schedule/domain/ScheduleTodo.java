package com.calendarbox.backend.schedule.domain;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EntityListeners(AuditingEntityListener.class)
public class ScheduleTodo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_todo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(nullable = false)
    private String content;

    @Column(name = "is_done", nullable = false)
    private boolean isDone;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;


    private ScheduleTodo(String content, boolean isDone, int orderNo){
        this.content = content;
        this.isDone = isDone;
        this.orderNo = orderNo;
    }

    public static ScheduleTodo of(String content, boolean isDone, int orderNo){
        return new ScheduleTodo(content,isDone,orderNo);
    }

    public static ScheduleTodo create(Schedule schedule, String content, int orderNo) {
        ScheduleTodo t = new ScheduleTodo();
        t.schedule = schedule;
        t.content = content;
        t.orderNo = orderNo;
        t.isDone = false;
        return t;
    }

    public void editContent(String content){ this.content = content; }
    public void makeDone(){ this.isDone = true; }
    public void makeNotDone(){ this.isDone = false; }
    public void setOrderNo(int orderNo){ this.orderNo = orderNo; }
    void setSchedule(Schedule schedule){ this.schedule = schedule; }
}
