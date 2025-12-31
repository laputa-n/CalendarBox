package com.calendarbox.backend.schedule.service;

import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.domain.ScheduleTodo;
import com.calendarbox.backend.schedule.dto.request.TodoCreateRequest;
import com.calendarbox.backend.schedule.dto.request.TodoReorderRequest;
import com.calendarbox.backend.schedule.dto.request.TodoUpdateRequest;
import com.calendarbox.backend.schedule.dto.response.TodoResponse;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import com.calendarbox.backend.schedule.repository.ScheduleTodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.calendarbox.backend.calendar.enums.CalendarMemberStatus.ACCEPTED;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleTodoService {

    private final CalendarMemberRepository calendarMemberRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ScheduleTodoRepository scheduleTodoRepository;
    private final ScheduleRepository scheduleRepository;

    public TodoResponse addAtBottom(Long userId, Long scheduleId, TodoCreateRequest req) {

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!s.getCreatedBy().getId().equals(userId)
                && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(s.getCalendar().getId(),userId,ACCEPTED)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(s.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        if (req.content() == null || req.content().isBlank())
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);

        int next = scheduleTodoRepository.findMaxOrderNo(scheduleId) + 1;

        var todo = ScheduleTodo.of(req.content().trim(),false, next);
        s.addTodo(todo);
        ScheduleTodo saved = scheduleTodoRepository.save(todo);
        return TodoResponse.from(saved);
    }

    public TodoResponse updateContent(Long userId, Long scheduleId, Long todoId, TodoUpdateRequest req) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(!s.getCreatedBy().getId().equals(userId)
                && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(s.getCalendar().getId(),userId,ACCEPTED)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(s.getId(),userId, ScheduleParticipantStatus.ACCEPTED)){
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        var t = getAndCheck(scheduleId, todoId);

        if (req.content() == null || req.content().isBlank())
            throw new BusinessException(ErrorCode.REQUEST_NO_CHANGES);

        t.editContent(req.content().trim());
        return TodoResponse.from(t);
    }

    public TodoResponse toggle(Long userId, Long scheduleId, Long todoId) {

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(!s.getCreatedBy().getId().equals(userId)
                && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(s.getCalendar().getId(),userId,ACCEPTED)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(s.getId(),userId, ScheduleParticipantStatus.ACCEPTED)){
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        var t = getAndCheck(scheduleId, todoId);
        if(t.isDone()) t.makeNotDone();
        else t.makeDone();
        return TodoResponse.from(t);
    }

    public void reorder(Long userId, Long scheduleId, TodoReorderRequest req) {
        if (req.orders() == null || req.orders().isEmpty()) return;

        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if (!s.getCreatedBy().getId().equals(userId)
                && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(s.getCalendar().getId(), userId, ACCEPTED)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(s.getId(), userId, ScheduleParticipantStatus.ACCEPTED)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        // 1) 입력 파싱
        Map<Long, Integer> desired = new HashMap<>();
        for (var it : req.orders()) {
            if (it.todoId() == null) throw new BusinessException(ErrorCode.INVALID_JSON);
            if (it.orderNo() < 0) throw new BusinessException(ErrorCode.INVALID_JSON);
            desired.put(it.todoId(), it.orderNo());
        }

        // 2) orderNo 중복 방지
        if (new HashSet<>(desired.values()).size() != desired.size()) {
            throw new BusinessException(ErrorCode.INVALID_JSON);
        }

        // 3) "전체 목록 reorder" 전제 검증 (권장)
        long total = scheduleTodoRepository.countBySchedule_Id(scheduleId);
        if (total != desired.size()) {
            throw new BusinessException(ErrorCode.INVALID_JSON); // 필요하면 전용 에러코드로 분리
        }

        // 4) orderNo 범위 검증 (0..N-1)
        int n = desired.size();
        for (int v : desired.values()) {
            if (v < 0 || v >= n) throw new BusinessException(ErrorCode.INVALID_JSON);
        }

        // 5) 대상 todo 조회 + 유효성 검증
        var todos = scheduleTodoRepository.findAllByIds(desired.keySet());

        // 존재하지 않는 todoId가 섞인 경우 방지
        if (todos.size() != desired.size()) {
            throw new BusinessException(ErrorCode.INVALID_JSON); // 또는 SCHEDULE_TODO_NOT_FOUND
        }

        // schedule 매칭 검증 + 1차 임시값 부여 (중간 유니크 충돌 방지)
        int tmp = Integer.MIN_VALUE;
        for (var t : todos) {
            if (!t.getSchedule().getId().equals(scheduleId)) {
                throw new BusinessException(ErrorCode.SCHEDULE_TODO_NOT_MATCH);
            }
            t.setOrderNo(tmp++); // 임시로 유니크한 음수 부여
        }

        // 임시값을 먼저 DB에 반영
        scheduleTodoRepository.flush();

        // 6) 최종 orderNo 반영
        for (var t : todos) {
            t.setOrderNo(desired.get(t.getId()));
        }
    }

    public void delete(Long userId, Long scheduleId, Long todoId) {
        Schedule s = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        if(!s.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(s.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);


        var t = getAndCheck(scheduleId, todoId);
        s.removeTodo(t);
    }

    private ScheduleTodo getAndCheck(Long scheduleId, Long todoId) {
        var t = scheduleTodoRepository.findById(todoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));
        if (!t.getSchedule().getId().equals(scheduleId))
            throw new BusinessException(ErrorCode.SCHEDULE_TODO_NOT_MATCH);
        return t;
    }
}
