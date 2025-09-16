package com.calendarbox.backend.schedule.validation;

import com.calendarbox.backend.schedule.dto.request.RecurrenceUpsertRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;

public class RecurrenceRuleValidator implements ConstraintValidator<ValidRecurrenceRule, RecurrenceUpsertRequest> {
    private static final Pattern BYDAY_TOKEN = Pattern.compile("(?:(?:[+-][1-5])?)(MO|TU|WE|TH|FR|SA|SU)");
    private static boolean empty(Collection<?> x){ return x==null || x.isEmpty(); }
    private static boolean inRange(Set<Integer> s, int lo, int hi, boolean excludeZero) {
        if (s==null) return true;
        for (int v: s) if ((excludeZero && v==0) || v<lo || v>hi) return false; return true;
    }

    @Override public boolean isValid(RecurrenceUpsertRequest r, ConstraintValidatorContext c) {
        if (r == null) return true;

        // 기본 범위
        if (r.intervalCount() == null || r.intervalCount() < 1) return fail(c, "interval must be >= 1");
        if (!inRange(r.byMonth(), 1, 12, false))                return fail(c, "bymonth must be 1..12");
        if (!inRange(r.byMonthday(), -31, 31, true))            return fail(c, "bymonthday must be -31..-1 or 1..31");
        if (r.byDay()!=null && !r.byDay().stream().allMatch(s -> BYDAY_TOKEN.matcher(s).matches()))
            return fail(c, "byday token invalid");

        // 조합 규칙
        switch (r.freq()) {
            case DAILY -> { /* ok */ }
            case WEEKLY -> { /* 비어도 OK: 서비스에서 dtstart 요일로 보정 */ }
            case MONTHLY -> {
                if (empty(r.byDay()) && empty(r.byMonthday()))
                    return fail(c, "MONTHLY requires byDay or byMonthday");
            }
            case YEARLY -> {
                if (empty(r.byMonth()))
                    return fail(c, "YEARLY requires byMonth");
                if (empty(r.byDay()) && empty(r.byMonthday()))
                    return fail(c, "YEARLY requires byDay or byMonthday");
            }
        }
        return true;
    }

    private boolean fail(ConstraintValidatorContext c, String msg){
        c.disableDefaultConstraintViolation();
        c.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
        return false;
    }
}

