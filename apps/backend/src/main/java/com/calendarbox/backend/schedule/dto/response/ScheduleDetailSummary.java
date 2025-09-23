package com.calendarbox.backend.schedule.dto.response;

public record ScheduleDetailSummary(
        boolean hasParticipants, Long participantCount
        ,boolean hasRecurrences, Long recurrenceCount
        ,boolean hasReminders, Long reminderCount
        ,boolean hasLinks, Long linkCount
        ,boolean hasTodos, Long todoCount
        ,boolean hasPlaces, Long placeCount
        ,boolean hasImg, Long imgCount
        ,boolean hasFiles, Long fileCount
) {
    public static ScheduleDetailSummary of(
            boolean hasParticipants,Long participantCount
            ,boolean hasRecurrences,Long recurrenceCount
            ,boolean hasReminders,Long reminderCount
            ,boolean hasLinks, Long linkCount
            ,boolean hasTodos, Long todoCount
            ,boolean hasPlaces, Long placeCount
            ,boolean hasImg, Long imgCount
            ,boolean hasFiles, Long fileCount
    ) {
        return new ScheduleDetailSummary(
         hasParticipants,  participantCount
        , hasRecurrences,  recurrenceCount
        , hasReminders,  reminderCount
        , hasLinks,  linkCount
        , hasTodos,  todoCount
        , hasPlaces,  placeCount
        , hasImg,  imgCount
        , hasFiles,  fileCount
        );
    }
}
