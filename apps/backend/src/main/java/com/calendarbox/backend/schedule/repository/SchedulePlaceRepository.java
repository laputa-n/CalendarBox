package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.SchedulePlace;
import com.calendarbox.backend.schedule.domain.ScheduleTodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface SchedulePlaceRepository extends JpaRepository<SchedulePlace, Long> {
    boolean existsByScheduleIdAndPlaceId(Long scheduleId, Long placeId);
    boolean existsByScheduleIdAndName(Long scheduleId, String name);

    @Query("select coalesce(max(sp.position), -1) from SchedulePlace sp where sp.schedule.id = :scheduleId")
    int findMaxPositionByScheduleId(Long scheduleId);

    @Query("select sp from SchedulePlace sp where sp.id in :ids")
    List<SchedulePlace> findAllByIds(Collection<Long> ids);

    List<SchedulePlace> findAllByScheduleId(Long scheduleId);
}
