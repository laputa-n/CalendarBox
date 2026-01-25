package com.calendarbox.backend.schedule.validation;

import com.calendarbox.backend.schedule.dto.request.RecurrenceUpsertRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RecurrenceRuleValidator implements ConstraintValidator<ValidRecurrenceRule, RecurrenceUpsertRequest> {

    /**
     * 서수(ordinal)와 요일을 모두 캡처하는 정규식.
     *  - group(1): ordinal (예: "3", "-1")  // 없으면 null
     *  - group(2): 요일 (MO/TU/WE/TH/FR/SA/SU)
     */
    private static final Pattern BYDAY_TOKEN = Pattern.compile("^([+-]?[1-5])?(MO|TU|WE|TH|FR|SA|SU)$");

    private static boolean empty(Collection<?> x){ return x==null || x.isEmpty(); }

    private static boolean inRange(Set<Integer> s, int lo, int hi, boolean excludeZero) {
        if (s==null) return true;
        for (int v: s){
            if ((excludeZero && v==0) || v<lo || v>hi) return false;
        }
        return true;
    }

    @Override
    public boolean isValid(RecurrenceUpsertRequest r, ConstraintValidatorContext c) {
        if (r == null) return true;

        // --- 공통 기본 범위 검증 ---
        if (r.intervalCount() == null || r.intervalCount() < 1)
            return fail(c, "interval must be >= 1");

        if (!inRange(r.byMonth(), 1, 12, false))
            return fail(c, "bymonth must be 1..12");

        if (!inRange(r.byMonthday(), -31, 31, true))
            return fail(c, "bymonthday must be -31..-1 or 1..31");

        // byDay 토큰 자체의 형식(요일 코드)은 우선 느슨하게 검사
        if (r.byDay()!=null && !r.byDay().stream().allMatch(s -> {
            String t = s==null? null : s.trim().toUpperCase(Locale.ROOT);
            return t!=null && BYDAY_TOKEN.matcher(t).matches();
        })) {
            return fail(c, "byday token invalid");
        }

        // --- 조합 규칙 ---
        switch (r.freq()) {
            case DAILY -> {
                // 제약 없음
            }
            case WEEKLY -> {
                // WEEKLY에서는 ordinal 없어도 허용 (예: 매주 월/수)
                // 필요 시 '비어있으면 seed 요일로 보정' 같은 정책은 서비스에서 처리
            }
            case MONTHLY -> {
                // MONTHLY는 둘 중 하나가 필수
                if (empty(r.byDay()) && empty(r.byMonthday()))
                    return fail(c, "MONTHLY requires byDay or byMonthday");

                //  MONTHLY에서 byDay를 쓰는 경우, 반드시 ordinal(서수)이 있어야 함: 3SU, -1WE 등의 형태만 허용
                if (!empty(r.byDay())) {
                    for (String raw : r.byDay()) {
                        if (raw == null) return fail(c, "MONTHLY byDay contains null");
                        Matcher m = BYDAY_TOKEN.matcher(raw.trim().toUpperCase(Locale.ROOT));
                        if (!m.matches())
                            return fail(c, "byday token invalid: " + raw);
                        // group(1)이 null이면 ordinal이 없는 것 → 금지
                        if (m.group(1) == null)
                            return fail(c, "MONTHLY byDay must include ordinal (e.g., 3SU or -1WE). Invalid: " + raw);
                    }
                }
            }
            case YEARLY -> {
                if (empty(r.byMonth())) return fail(c, "YEARLY requires byMonth");
                if (empty(r.byDay()) && empty(r.byMonthday()))
                    return fail(c, "YEARLY requires byDay or byMonthday");

                // 주석 해제: YEARLY에서도 byDay에 ordinal 필수
                if (!empty(r.byDay())) {
                    for (String raw : r.byDay()) {
                        if (raw == null) return fail(c, "YEARLY byDay contains null");
                        Matcher m = BYDAY_TOKEN.matcher(raw.trim().toUpperCase(Locale.ROOT));
                        if (!m.matches() || m.group(1) == null)
                            return fail(c, "YEARLY byDay must include ordinal (e.g., 2MO or -1FR). Invalid: " + raw);
                    }
                }
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
