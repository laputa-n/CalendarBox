package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.schedule.domain.ScheduleReminder;

public record ReminderResponse(Long scheduleReminderId, int minutesBefore) {
}
