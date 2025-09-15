package com.calendarbox.backend.schedule.dto.response;

import com.calendarbox.backend.schedule.domain.ScheduleTodo;

import java.time.Instant;

public record TodoResponse(
        Long scheduleTodoId, String content, boolean isDone, int orderNo,
        Instant createdAt, Instant updatedAt
) {
    public static TodoResponse from(ScheduleTodo t){
        return new TodoResponse(t.getId(),t.getContent(),t.isDone(),t.getOrderNo(),t.getCreatedAt(),t.getUpdatedAt());
    }
}
