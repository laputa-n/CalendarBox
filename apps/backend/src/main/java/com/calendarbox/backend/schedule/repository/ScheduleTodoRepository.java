package com.calendarbox.backend.schedule.repository;

import com.calendarbox.backend.schedule.domain.ScheduleTodo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ScheduleTodoRepository extends JpaRepository<ScheduleTodo, Long> {
    @Query("""
      select t from ScheduleTodo t
      where t.schedule.id = :scheduleId
      order by t.orderNo asc, t.id asc
    """)
    List<ScheduleTodo> findByScheduleIdOrderByOrder(Long scheduleId);

    @Query("select coalesce(max(t.orderNo), -1) from ScheduleTodo t where t.schedule.id = :scheduleId")
    int findMaxOrderNo(Long scheduleId);

    @Query("select t from ScheduleTodo t where t.id in :ids")
    List<ScheduleTodo> findAllByIds(Collection<Long> ids);
}
