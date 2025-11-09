package com.calendarbox.backend.expense.service;

import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.expense.domain.Expense;
import com.calendarbox.backend.expense.dto.response.ExpenseDetailResponse;
import com.calendarbox.backend.expense.dto.response.ExpenseListItem;
import com.calendarbox.backend.expense.dto.response.ExpenseListResponse;
import com.calendarbox.backend.expense.repository.ExpenseRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseQueryService {
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;
    private final ExpenseRepository expenseRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final CalendarMemberRepository calendarMemberRepository;
    public ExpenseListResponse getExpenses(Long userId, Long scheduleId){
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!schedule.getCreatedBy().getId().equals(userId)
        && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED)
        && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(schedule.getCalendar().getId(),userId, CalendarMemberStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        List<Expense> expenses =  expenseRepository.findBySchedule_Id(scheduleId);
        int cnt = expenses.size();
        long totalAmount = expenses.stream().mapToLong(Expense::getAmount).sum();
        List<ExpenseListItem> items = expenses.stream().map(
                e -> new ExpenseListItem(
                            e.getId(),
                            e.getName(),
                            e.getAmount(),
                            e.getPaidAt(),
                            e.getOccurrenceDate(),
                            e.getSource()
                    )).toList();

        return new ExpenseListResponse(
                cnt,totalAmount,items
        );
    }

    public ExpenseDetailResponse getDetail(Long userId, Long scheduleId, Long expenseId){
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!schedule.getCreatedBy().getId().equals(userId)
                && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED)
                && !calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(schedule.getCalendar().getId(),userId, CalendarMemberStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));

        return ExpenseDetailResponse.from(expense);
    }
}
