package com.calendarbox.backend.expense.service;

import com.calendarbox.backend.attachment.domain.Attachment;
import com.calendarbox.backend.attachment.repository.AttachmentRepository;
import com.calendarbox.backend.expense.dto.request.AddExpenseRequest;
import com.calendarbox.backend.expense.dto.request.EditExpenseRequest;
import com.calendarbox.backend.expense.dto.response.AddExpenseResponse;
import com.calendarbox.backend.expense.domain.Expense;
import com.calendarbox.backend.expense.dto.response.ExpenseDetailResponse;
import com.calendarbox.backend.expense.repository.ExpenseRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.global.infra.storage.StorageClient;
import com.calendarbox.backend.member.domain.Member;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.enums.ScheduleParticipantStatus;
import com.calendarbox.backend.schedule.repository.ScheduleParticipantRepository;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {
    private final ScheduleParticipantRepository scheduleParticipantRepository;
    private final MemberRepository memberRepository;
    private final ScheduleRepository scheduleRepository;
    private final ExpenseRepository expenseRepository;
    private final StorageClient storageClient;
    private final AttachmentRepository attachmentRepository;

    public AddExpenseResponse addExpense(Long userId, Long scheduleId,AddExpenseRequest req){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!schedule.getCreatedBy().getId().equals(userId) && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        Expense expense = Expense.fromManual(schedule, req.name(),req.amount(),req.paidAt(),req.occurrenceDate());
        schedule.addExpense(expense);
        expenseRepository.save(expense);

        return new AddExpenseResponse(expense.getId(), scheduleId, expense.getName(),expense.getAmount(),expense.getPaidAt(),expense.getOccurrenceDate(),expense.getCreatedAt(),expense.getSource());
    }

    public ExpenseDetailResponse editExpense(Long userId, Long scheduleId, Long expenseId, EditExpenseRequest req){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!schedule.getCreatedBy().getId().equals(userId) && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
        if(req.name() != null && !req.name().equals(expense.getName())) expense.changeName(req.name());
        if(req.amount() != null && !req.amount().equals(expense.getAmount())) expense.chageAmount(req.amount());
        if(req.paidAt() != null && !req.paidAt().equals(expense.getPaidAt())) expense.chagePaidAt(req.paidAt());

        return ExpenseDetailResponse.from(expense);
    }

    public void deleteExpense(Long userId, Long scheduleId, Long expenseId){
        Member user = memberRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));

        if(!schedule.getCreatedBy().getId().equals(userId) && !scheduleParticipantRepository.existsBySchedule_IdAndMember_IdAndStatus(scheduleId,userId, ScheduleParticipantStatus.ACCEPTED))
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);

        Expense expense = expenseRepository.findById(expenseId).orElseThrow(() -> new BusinessException(ErrorCode.EXPENSE_NOT_FOUND));
        for(var att: expense.getAttachments()){
            Attachment attachment = att.getAttachment();
            storageClient.deleteQuietly(attachment.getObjectKey());
            attachmentRepository.delete(attachment);
        }

        schedule.removeExpense(expense);
        expenseRepository.delete(expense);
    }
}
