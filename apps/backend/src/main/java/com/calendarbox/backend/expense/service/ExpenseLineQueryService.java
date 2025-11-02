package com.calendarbox.backend.expense.service;

import com.calendarbox.backend.expense.domain.Expense;
import com.calendarbox.backend.expense.dto.response.ExpenseLineListResponse;
import com.calendarbox.backend.expense.repository.ExpenseRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseLineQueryService {
    private final MemberRepository memberRepository;
    private final ExpenseRepository expenseRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;

    public ExpenseLineListResponse getLines(Long userId, Long expenseId){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
        Schedule schedule = expense.getSchedule();

        if(!schedule.getCreatedBy().getId().equals(userId) && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(schedule.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        return ExpenseLineListResponse.from(expense);
    }
}
