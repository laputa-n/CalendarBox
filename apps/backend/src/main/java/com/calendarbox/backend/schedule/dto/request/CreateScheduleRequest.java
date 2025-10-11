package com.calendarbox.backend.schedule.dto.request;

import com.calendarbox.backend.schedule.enums.ScheduleTheme;
import com.calendarbox.backend.schedule.validation.ValidRecurrenceRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CreateScheduleRequest(
        @NotNull String title,
        String memo,
        ScheduleTheme theme,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @Valid List<LinkReq> links,
        @Valid List<PlaceReq>        places,
        @Valid List<ParticipantReq>  participants,
        @Valid List<TodoReq>         todos,
        @Valid List<ReminderReq>     reminders,
        @Valid List<AttachmentReq>   attachments,
        @Valid @ValidRecurrenceRule  RecurrenceUpsertRequest recurrence
) {
        public record LinkReq(
                @NotBlank String url,
                String label
        ){}

        public record TodoReq(
                @NotBlank String content,
                boolean isDone,
                @NotNull Integer orderNo
        ){}

        public record ReminderReq(
                @Min(0) Integer minutesBefore
        ){}

        public record PlaceReq(
                Long placeId,                  // 우선순위 높음
                String name,                   // 자유 입력
                String address,
                String roadAddress,
                Double lat, Double lng,
                String provider,               // NAVER/KAKAO 등
                String providerPlaceKey,       // 외부키
                Integer position               // 정렬
        ){}

        // 참가자도 두 경로: 회원 참조 OR 이름만
        public record ParticipantReq(
                Long memberId,
                String name,                   // 비회원 이름
                @NotNull String status         // INVITED/ACCEPTED/REJECTED
        ){}

        public record AttachmentReq(
                @NotBlank String objectKey,
                @NotBlank String originalName,
                @NotBlank String mimeType,
                @NotNull Long byteSize,
                boolean isImg,
                Integer position
        ){}
}
