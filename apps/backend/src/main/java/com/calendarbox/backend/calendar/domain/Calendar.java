package com.calendarbox.backend.calendar.domain;

import com.calendarbox.backend.calendar.enums.CalendarType;
import com.calendarbox.backend.calendar.enums.Visibility;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
        indexes = {
                @Index(name = "idx_calendar_owner", columnList = "owner_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_calendar_owner_name", columnNames = {"owner_id", "name"})
        }
)
public class Calendar {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calendar_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Column(nullable = false, length = 100)
    @NotBlank
    @Size(max = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CalendarType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Calendar(Member owner, String name, CalendarType type, Visibility visibility) {
        this.owner = Objects.requireNonNull(owner);
        this.name = name;
        this.type = Objects.requireNonNull(type);
        this.visibility = Objects.requireNonNull(visibility);
    }

    public static Calendar create(Member owner, String name, CalendarType type, Visibility visibility) {
        CalendarType ct = type == null?CalendarType.PERSONAL:type;
        Visibility v = visibility == null?Visibility.PRIVATE:visibility;
        return new Calendar(owner, name, ct, v);
    }

    public void rename(String name){
        if(name == null || name.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        this.name = name;
    }

    public void changeVisibility(Visibility visibility){
        this.visibility = Objects.requireNonNull(visibility);
    }

    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
    }
}
