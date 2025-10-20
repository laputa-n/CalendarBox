package com.calendarbox.backend.schedule.domain;

import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.place.domain.Place;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@EntityListeners(AuditingEntityListener.class)
public class SchedulePlace {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_place_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(nullable = false)
    private int position = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Builder
    public SchedulePlace(Schedule schedule,Place place, String name, int position) {
        this.schedule = schedule;
        this.place = place;
        this.name = name;
        this.position = position;
    }

    public void changeName(String s){
        if(s == null || s.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        this.name = s;
    }

    public void changePosition(int i){
        if(i < 0) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        this.position = i;
    }

    void setSchedule(Schedule schedule){
        this.schedule = schedule;
    }
}
