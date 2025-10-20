package com.calendarbox.backend.friendship.domain;

import com.calendarbox.backend.friendship.enums.FriendshipStatus;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Friendship {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="friendship_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private Member requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    private Member addressee;

    @Enumerated(EnumType.STRING)
    @Column(name="status", length = 100, nullable = false)
    private FriendshipStatus status;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name="responded_at")
    private Instant respondedAt;


    public static Friendship request(Member requester, Member addressee){
        if (requester == null || addressee == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "요청/대상 회원이 비어있습니다.");
        }
        if (requester.getId().equals(addressee.getId())) {
            throw new BusinessException(ErrorCode.FRIENDSHIP_SELF_REQUEST);
        }
        Friendship f = new Friendship();
        f.requester = requester;
        f.addressee = addressee;
        f.status = FriendshipStatus.PENDING;
        return f;
    }

    public void accept(){
        ensurePending();
        status = FriendshipStatus.ACCEPTED;
    }

    public void reject(){
        ensurePending();
        status = FriendshipStatus.REJECTED;
    }

    private void ensurePending(){
        if(this.status != FriendshipStatus.PENDING){
            throw new BusinessException(ErrorCode.FRIENDSHIP_INVALID_STATE, this.status);
        }
    }
    @PrePersist
    void onCreate(){
        this.createdAt = Instant.now();
    }

    @PreUpdate
    void onUpdate(){
        if (this.status != FriendshipStatus.PENDING) {
            this.respondedAt = Instant.now();
        }
    }
}


