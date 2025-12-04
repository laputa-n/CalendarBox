package com.calendarbox.backend.place.util;

import com.calendarbox.backend.schedule.enums.ScheduleCategory;

public interface PlaceCategoryWeigher {
    double weight(ScheduleCategory scheduleCategory, String placeCategory);
}
