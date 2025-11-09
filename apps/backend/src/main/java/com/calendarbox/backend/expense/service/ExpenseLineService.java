package com.calendarbox.backend.expense.service;

import com.calendarbox.backend.expense.domain.Expense;
import com.calendarbox.backend.expense.domain.ExpenseLine;
import com.calendarbox.backend.expense.dto.request.AddExpenseLineRequest;
import com.calendarbox.backend.expense.dto.request.EditExpenseLineRequest;
import com.calendarbox.backend.expense.dto.response.ExpenseLineDto;
import com.calendarbox.backend.expense.repository.ExpenseLineRepository;
import com.calendarbox.backend.expense.repository.ExpenseRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import com.calendarbox.backend.schedule.service.ScheduleReminderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseLineService {
    private final MemberRepository memberRepository;
    private final ExpenseRepository expenseRepository;
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final ExpenseLineRepository expenseLineRepository;

    public ExpenseLineDto addExpenseLine(Long userId, Long expenseId, AddExpenseLineRequest request){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
        Schedule schedule = expense.getSchedule();

        if(!schedule.getCreatedBy().getId().equals(userId) && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(schedule.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        ExpenseLine expenseLine = ExpenseLine.of(expense, request.label(),request.resolvedQuantity(),request.resolvedUnitAmount(),request.lineAmount());

        expense.addLine(expenseLine);
        expenseLineRepository.save(expenseLine);

        return ExpenseLineDto.of(expenseLine);
    }

    public ExpenseLineDto editExpenseLine(Long userId, Long expenseId, Long expenseLineId, EditExpenseLineRequest request){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
        Schedule schedule = expense.getSchedule();

        if(!schedule.getCreatedBy().getId().equals(userId) && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(schedule.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        ExpenseLine expenseLine = expenseLineRepository.findById(expenseLineId).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_LINE_NOT_FOUND));

        if (!Objects.equals(request.label(), expenseLine.getLabel()))
            expenseLine.changeLabel(request.label());
        if (!Objects.equals(request.quantity(), expenseLine.getQuantity()))
            expenseLine.changeQuantity(request.quantity());
        if (!Objects.equals(request.unitAmount(), expenseLine.getUnitAmount()))
            expenseLine.changeUnitAmount(request.unitAmount());
        if (!Objects.equals(request.lineAmount(), expenseLine.getLineAmount()))
            expenseLine.changeLineAmount(request.lineAmount());

        long lineAmount = Optional.ofNullable(expenseLine.getLineAmount()).orElse(0L);
        long unitAmount = Optional.ofNullable(expenseLine.getUnitAmount()).orElse(0L);
        int qty = Optional.ofNullable(expenseLine.getQuantity()).orElse(1);

        if (lineAmount != unitAmount * qty)
            throw new BusinessException(ErrorCode.LINE_AMOUNT_NOT_MATCH);

        return ExpenseLineDto.of(expenseLine);
    }

    public void deleteExpenseLine(Long userId, Long expenseId, Long expenseLineId){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
        Schedule schedule = expense.getSchedule();

        if(!schedule.getCreatedBy().getId().equals(userId) && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(schedule.getId(),userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        ExpenseLine expenseLine = expenseLineRepository.findById(expenseLineId).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_LINE_NOT_FOUND));

        expense.removeLine(expenseLine);
    }
}
