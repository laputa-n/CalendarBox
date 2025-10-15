package com.calendarbox.backend.occurrence.service;

import com.calendarbox.backend.calendar.enums.CalendarMemberStatus;
import com.calendarbox.backend.calendar.repository.CalendarMemberRepository;
import com.calendarbox.backend.global.error.BusinessException;
import com.calendarbox.backend.global.error.ErrorCode;
import com.calendarbox.backend.member.repository.MemberRepository;
import com.calendarbox.backend.occurrence.dto.response.OccurrenceBucketResponse;
import com.calendarbox.backend.occurrence.dto.response.OccurrenceItem;
import com.calendarbox.backend.occurrence.support.OccurrenceSplitter;
import com.calendarbox.backend.occurrence.support.RecurrenceExpander;
import com.calendarbox.backend.schedule.domain.Schedule;
import com.calendarbox.backend.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OccurrenceQueryService {

    private final ScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final CalendarMemberRepository calendarMemberRepository;

    // 이미 사용 중인 헬퍼로 보임(너 코드에 등장)
    private final OccurrenceSplitter occurrenceSplitter;
    private final RecurrenceExpander recurrenceExpander;

    public OccurrenceBucketResponse getOccurrences(
            Long viewerId, Long calendarIdOrNull,
            ZonedDateTime fromZ, ZonedDateTime toZ, ZoneId zone
    ){
        // 0) 사용자 존재 확인
        memberRepository.findById(viewerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 1) 대상 캘린더 집합 계산 + 권한 검사
        //    - calendarId 지정: 그 캘린더 권한 체크(ACCEPTED)
        //    - null: 내가 ACCEPTED로 속한 모든 캘린더 목록
        List<Long> targetCalendarIds = resolveTargetCalendarIds(viewerId, calendarIdOrNull);


        // 2) UTC 변환 (저장은 UTC 가정)
        Instant fromUtc = fromZ.toInstant();
        Instant toUtc   = toZ.toInstant();

        //가드
        if (targetCalendarIds.isEmpty()) {
            return new OccurrenceBucketResponse(calendarIdOrNull, fromUtc, toUtc, Map.of());
        }

        // 3) 데이터 로드
        List<Schedule> singles = scheduleRepository.findNoRecurByCalendarIds(targetCalendarIds, fromUtc, toUtc);
        List<Schedule> recurrings = scheduleRepository.findRecurringWithExceptionsByCalendarIds(targetCalendarIds, fromUtc);

        // 4) 전개
        List<OccurrenceItem> out = new ArrayList<>();

        // 4-1) 단발: 멀티데이 split(표시 안정성) 후 item화
        for (Schedule s : singles) {
            for (var split : occurrenceSplitter.splitIfMultiDay(s.getStartAt(), s.getEndAt(), zone)) {
                out.add(toItemSingle(s, split.startUtc(), split.endUtc()));
            }
        }

        // 4-2) 반복: 예외 적용 + 윈도우 전개
        for (Schedule s : recurrings) {
            var r = s.getRecurrence(); // LAZY여도 fetch-join으로 미리 로드됨(리포지 쿼리 참조)
            var occs = recurrenceExpander.expand(s, r, fromZ, toZ, zone);
            for (var occ : occs) {
                out.add(toItemRecurring(s, occ.startUtc(), occ.endUtc()));
            }
        }

        // 5) 날짜별 그룹핑 (현지날짜 기준)
        Map<LocalDate, List<OccurrenceItem>> days = out.stream()
                .sorted(Comparator.comparing(OccurrenceItem::startAtUtc)) // 표시 안정성
                .collect(Collectors.groupingBy(
                        it -> it.startAtUtc().atZone(zone).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // calendarId는 단건/다건 모두 대응 위해 null이면 -1 같은 표식도 가능하지만
        // 그대로 null 허용 대신 days만 신뢰해도 됨. 여긴 기존 레코드 유지.
        return new OccurrenceBucketResponse(
                calendarIdOrNull, fromUtc, toUtc, days
        );
    }

    private List<Long> resolveTargetCalendarIds(Long viewerId, Long calendarIdOrNull) {
        if (calendarIdOrNull != null) {
            if(!calendarMemberRepository.existsByCalendar_IdAndMember_IdAndStatus(calendarIdOrNull, viewerId, CalendarMemberStatus.ACCEPTED)) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
            return List.of(calendarIdOrNull);
        }
        return calendarMemberRepository.findCalendarIdsByMemberIdAndStatuses(viewerId,List.of(CalendarMemberStatus.ACCEPTED));
    }

    private OccurrenceItem toItemSingle(Schedule s, Instant st, Instant et) {
        return new OccurrenceItem(
                s.getId() + "@" + st.toString(),
                s.getId(),
                s.getCalendar().getId(),
                s.getTitle(),
                s.getTheme().name(),
                st, et,
                false
        );
    }

    private OccurrenceItem toItemRecurring(Schedule s, Instant st, Instant et) {
        return new OccurrenceItem(
                s.getId() + "@" + st.toString(),
                s.getId(),
                s.getCalendar().getId(),
                s.getTitle(),
                s.getTheme().name(),
                st, et,
                true
        );
    }
}
