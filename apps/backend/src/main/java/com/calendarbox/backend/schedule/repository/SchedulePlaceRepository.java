package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.place.domain.Place;
import com.calendarbox.backend.schedule.domain.SchedulePlace;
import com.calendarbox.backend.schedule.domain.ScheduleTodo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SchedulePlaceRepository extends JpaRepository<SchedulePlace, Long> {

    interface SchedulePlaceProjection{
        Long getScheduleId();
        Place getPlace();
    }
    boolean existsByScheduleIdAndPlaceId(Long scheduleId, Long placeId);
    boolean existsByScheduleIdAndName(Long scheduleId, String name);
    boolean existsBySchedule_Id(Long scheduleId);

    @Query("select coalesce(max(sp.position), -1) from SchedulePlace sp where sp.schedule.id = :scheduleId")
    int findMaxPositionByScheduleId(Long scheduleId);

    @Query("select sp from SchedulePlace sp where sp.id in :ids")
    List<SchedulePlace> findAllByIds(Collection<Long> ids);

    @EntityGraph(attributePaths = {"place"})
    List<SchedulePlace> findAllByScheduleIdOrderByPositionAsc(Long scheduleId);

    @Modifying
    @Query(value = """
        INSERT INTO schedule_place (schedule_id, place_id, name, position)
        SELECT :dstId, place_id, name, position
        FROM schedule_place
        WHERE schedule_id = :srcId
        """, nativeQuery = true)
    void copyAllForClone(@Param("srcId") Long srcId, @Param("dstId") Long dstId);

    Long countBySchedule_Id(Long scheduleId);

    @Query("""
        select sp.schedule.id as scheduleId, sp.place as place
        from SchedulePlace sp
        where sp.schedule.id in :scheduleIds
            and sp.place is not null
    """)
    List<SchedulePlaceProjection> findByScheduleIds(List<Long> scheduleIds);
}

