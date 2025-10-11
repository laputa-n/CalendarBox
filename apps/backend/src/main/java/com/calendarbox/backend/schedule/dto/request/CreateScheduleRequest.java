package com.calendarbox.backend.schedule.dto.request;

import com.calendarbox.backend.schedule.enums.AddParticipantMode;
import com.calendarbox.backend.schedule.enums.ScheduleTheme;
import com.calendarbox.backend.schedule.validation.ValidRecurrenceRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateScheduleRequest(
        @NotNull String title,
        String memo,
        ScheduleTheme theme,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @Valid List<LinkReq> links,
        @Valid List<AddSchedulePlaceRequest> places,
        @Valid List<ParticipantReq>  participants,
        @Valid List<TodoReq>         todos,
        @Valid List<ReminderReq>     reminders,
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

        public record ParticipantReq(
                @NotNull AddParticipantMode mode,
                Long memberId,
                String name
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
