// src/components/pages/Dashboard.js
import React, { useEffect, useMemo, useState, useCallback } from 'react';
import { Calendar as CalendarIcon } from 'lucide-react';

import { useAuth } from '../../contexts/AuthContext';
import { useCalendars } from '../../contexts/CalendarContext';
import { useFriends } from '../../contexts/FriendContext';
import { useNotifications } from '../../contexts/NotificationContext';

import { formatDate, formatTime, isToday } from '../../utils/dateUtils';

import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';

import ScheduleModal from '../ScheduleModal/ScheduleModal';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ApiService } from '../../services/apiService';

/**
 * FullCalendar가 주는 Date(start/end)를
 * ISO8601 +09:00 형식으로 만들어 query로 보낸다.
 *
 * 예: 2025-10-01T00:00:00+09:00
 */
const toISOWithKstOffset = (date) => {
  const pad = (n) => String(n).padStart(2, '0');

  const yyyy = date.getFullYear();
  const mm = pad(date.getMonth() + 1);
  const dd = pad(date.getDate());
  const hh = pad(date.getHours());
  const mi = pad(date.getMinutes());
  const ss = pad(date.getSeconds());

  // KST 고정
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}+09:00`;
};

/**
 * occurrences 응답(data.days)을 FullCalendar events로 flatten
 * days: { "YYYY-MM-DD": [ { occurrenceId, scheduleId, ... } ] }
 */
const flattenDaysToEvents = (daysObj) => {
  if (!daysObj || typeof daysObj !== 'object') return [];

  const events = [];
  for (const dayKey of Object.keys(daysObj)) {
    const list = Array.isArray(daysObj[dayKey]) ? daysObj[dayKey] : [];

    for (const o of list) {
      if (!o) continue;

      // theme -> 색상 매핑 (원하면 너 스타일대로 바꿔도 됨)
      const themeColorMap = {
        BLUE: '#3b82f6',
        GREEN: '#10b981',
        PURPLE: '#8b5cf6',
        ORANGE: '#f59e0b',
        RED: '#ef4444',
        GRAY: '#6b7280',
      };
      const color = themeColorMap[o.theme] || '#3b82f6';

      // FullCalendar는 ISO 문자열(UTC Z) 넣어도 로컬(KST)로 표시해줌
      // allDay가 true면 end 없이 start만 줘도 되고, 날짜만 줘도 됨.
      const start = o.allDay ? dayKey : o.startAtUtc;
      const end = o.allDay ? undefined : o.endAtUtc;

      events.push({
        id: o.occurrenceId,     // 유니크
        title: o.title,
        start,
        end,
        allDay: !!o.allDay,
        backgroundColor: color,
        borderColor: color,
        extendedProps: {
          // 수정/삭제/상세에서 필요할 값들
          occurrenceId: o.occurrenceId,
          scheduleId: o.scheduleId,
          calendarId: o.calendarId,
          theme: o.theme,
          startAtUtc: o.startAtUtc,
          endAtUtc: o.endAtUtc,
          recurring: !!o.recurring,
          dayKey, // KST 기준 날짜키
          color,
        },
      });
    }
  }
  return events;
};

export const Dashboard = () => {
  const { user } = useAuth();
  const { calendars, refreshCalendars, loading } = useCalendars();
  const { acceptedFriendships } = useFriends();
  const { unreadCount } = useNotifications();

  const [defaultCalendar, setDefaultCalendar] = useState(null);

  // ✅ FullCalendar events (occurrences 기반)
  const [calendarEvents, setCalendarEvents] = useState([]);
  const [occLoading, setOccLoading] = useState(false);

  // ✅ 모달 상태
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedOccurrence, setSelectedOccurrence] = useState(null);

  // ✅ 최초 캘린더 로드
  useEffect(() => {
    refreshCalendars();
  }, []);

  // ✅ 기본 캘린더 설정
useEffect(() => {
  if (Array.isArray(calendars)) {
    const def = calendars.find((c) => c.isDefault);

    // ✅ calendarId를 id로도 쓸 수 있게 맞춰줌
    if (def) {
      setDefaultCalendar({
        ...def,
        id: def.id ?? def.calendarId,
      });
    } else {
      setDefaultCalendar(null);
    }
  }
}, [calendars]);

const fetchOccurrences = useCallback(async ({ start, end }) => {
  try {
    const defaultCalendarId = defaultCalendar?.id ?? defaultCalendar?.calendarId;
    if (!defaultCalendarId) return;

    setOccLoading(true);

    const fromKst = toISOWithKstOffset(start);
    const toKst = toISOWithKstOffset(end);

    const res = await ApiService.getAllOccurrences({ fromKst, toKst });

    const root = res?.data ?? res;

    const days =
      root?.data?.days ??  
      root?.days ??       
      {};

    const events = flattenDaysToEvents(days);

    setCalendarEvents(events);
  } catch (e) {
    console.error('❌ [Dashboard] getAllOccurrences failed:', e);
    setCalendarEvents([]);
  } finally {
    setOccLoading(false);
  }
}, [defaultCalendar]);


  // ✅ 날짜 클릭 → 생성 모드
  const handleDateClick = (info) => {
    setSelectedDate(info.dateStr); // YYYY-MM-DD
    setSelectedOccurrence(null);
    setIsModalOpen(true);
  };

  // ✅ 이벤트 클릭 → 수정 모드(occurrence를 그대로 넘김)
  const handleEventClick = (info) => {
    const ext = info?.event?.extendedProps || {};

    // ScheduleModal이 scheduleId를 뽑아 edit 모드로 들어가게 eventData 구성
    const occ = {
      occurrenceId: ext.occurrenceId,
      scheduleId: ext.scheduleId,
      calendarId: ext.calendarId,
      title: info.event.title,
      theme: ext.theme,
      startAtUtc: ext.startAtUtc,
      endAtUtc: ext.endAtUtc,
      recurring: ext.recurring,
      allDay: info.event.allDay,
      occurrenceDate: ext.dayKey, // KST LocalDate (YYYY-MM-DD)
      color: ext.color,
    };

    setSelectedOccurrence(occ);
    setSelectedDate(null);
    setIsModalOpen(true);
  };

  /**
   * ✅ 오른쪽 패널(오늘/다가오는)은 calendarEvents 기반으로 계산
   * - calendarEvents의 start는 UTC(Z) 또는 dayKey(올데이)라서 Date 변환에 주의
   */
  const todayEvents = useMemo(() => {
    return calendarEvents.filter((ev) => {
      const start = ev.allDay ? `${ev.extendedProps.dayKey}T00:00:00` : ev.start;
      return isToday(start);
    });
  }, [calendarEvents]);

  const upcomingEvents = useMemo(() => {
    return calendarEvents
      .filter((ev) => {
        const start = ev.allDay ? `${ev.extendedProps.dayKey}T00:00:00` : ev.start;
        return new Date(start) > new Date();
      })
      .sort((a, b) => {
        const as = a.allDay ? `${a.extendedProps.dayKey}T00:00:00` : a.start;
        const bs = b.allDay ? `${b.extendedProps.dayKey}T00:00:00` : b.start;
        return new Date(as) - new Date(bs);
      })
      .slice(0, 5);
  }, [calendarEvents]);

  const cardStyle = {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb'
  };

  if (loading) return <LoadingSpinner text="대시보드를 불러오는 중..." />;

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif' }}>
      {/* 헤더 */}
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>
          안녕하세요, {user?.name || '사용자'}님 👋
        </h1>
        <p style={{ color: '#6b7280' }}>오늘도 좋은 하루 되세요 ✨</p>
      </div>

      {/* 통계 카드 */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '1rem',
          marginBottom: '2rem'
        }}
      >
        <div style={{ ...cardStyle, background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)', color: 'white' }}>
          <p style={{ fontSize: '0.75rem' }}>오늘의 일정</p>
          <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{todayEvents.length}</p>
        </div>

        <div style={{ ...cardStyle, background: 'linear-gradient(135deg, #10b981 0%, #047857 100%)', color: 'white' }}>
          <p style={{ fontSize: '0.75rem' }}>친구 수</p>
          <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{acceptedFriendships.length}</p>
        </div>

        <div style={{ ...cardStyle, background: 'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)', color: 'white' }}>
          <p style={{ fontSize: '0.75rem' }}>캘린더 수</p>
          <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{calendars.length}</p>
        </div>

        <div style={{ ...cardStyle, background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)', color: 'white' }}>
          <p style={{ fontSize: '0.75rem' }}>읽지 않은 알림</p>
          <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{unreadCount}</p>
        </div>
      </div>

      {/* 메인 콘텐츠 */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
        {/* 캘린더 */}
        <div style={cardStyle}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '1rem' }}>
            📅 전체 일정 캘린더
          </h2>

          {!defaultCalendar ? (
            <div style={{ textAlign: 'center', color: '#6b7280', padding: '3rem' }}>
              <CalendarIcon size={48} style={{ marginBottom: '1rem' }} />
              <p>캘린더와 일정이 없습니다.</p>
              <p>‘캘린더 관리’에서 캘린더를 생성해주세요.</p>
            </div>
          ) : (
            <div style={{ position: 'relative' }}>
              {occLoading && (
                <div style={{
                  position: 'absolute',
                  top: 10,
                  right: 10,
                  zIndex: 10,
                  background: 'rgba(255,255,255,0.9)',
                  border: '1px solid #e5e7eb',
                  padding: '0.5rem 0.75rem',
                  borderRadius: '0.5rem',
                  fontSize: '0.875rem',
                  color: '#374151'
                }}>
                  불러오는 중...
                </div>
              )}

              <FullCalendar
                plugins={[dayGridPlugin, interactionPlugin]}
                initialView="dayGridMonth"
                locale="ko"
                height="80vh"
                events={calendarEvents}
                datesSet={(arg) => fetchOccurrences({ start: arg.start, end: arg.end })}
                dateClick={handleDateClick}
                eventClick={handleEventClick}
                headerToolbar={{
                  left: 'prev,next today',
                  center: 'title',
                  right: 'dayGridMonth,dayGridWeek,dayGridDay',
                }}
              />
            </div>
          )}
        </div>

        {/* 오른쪽 패널 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* 오늘 */}
          <div style={cardStyle}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
              오늘의 일정
            </h3>

            {todayEvents.length > 0 ? (
              todayEvents.map((ev) => {
                const start = ev.allDay ? `${ev.extendedProps.dayKey}T00:00:00` : ev.start;
                const end = ev.allDay ? null : ev.end;

                return (
                  <div
                    key={ev.id}
                    onClick={() => {
                      // 사이드 클릭도 수정 모달
                      setSelectedOccurrence({
                        occurrenceId: ev.extendedProps.occurrenceId,
                        scheduleId: ev.extendedProps.scheduleId,
                        calendarId: ev.extendedProps.calendarId,
                        title: ev.title,
                        theme: ev.extendedProps.theme,
                        startAtUtc: ev.extendedProps.startAtUtc,
                        endAtUtc: ev.extendedProps.endAtUtc,
                        recurring: ev.extendedProps.recurring,
                        allDay: ev.allDay,
                        occurrenceDate: ev.extendedProps.dayKey,
                        color: ev.extendedProps.color,
                      });
                      setSelectedDate(null);
                      setIsModalOpen(true);
                    }}
                    style={{
                      marginBottom: '0.5rem',
                      padding: '0.75rem',
                      backgroundColor: '#f9fafb',
                      borderLeft: `4px solid ${ev.extendedProps.color || '#3b82f6'}`,
                      borderRadius: '0.5rem',
                      cursor: 'pointer',
                    }}
                  >
                    <p style={{ fontWeight: '600', margin: 0 }}>{ev.title}</p>
                    <p style={{ fontSize: '0.75rem', color: '#6b7280', margin: 0 }}>
                      {ev.allDay ? '하루종일' : `${formatTime(start)} ~ ${formatTime(end)}`}
                    </p>
                  </div>
                );
              })
            ) : (
              <p style={{ color: '#6b7280', textAlign: 'center', padding: '1rem' }}>
                오늘 일정이 없습니다
              </p>
            )}
          </div>

          {/* 다가오는 */}
          <div style={cardStyle}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
              다가오는 일정
            </h3>

            {upcomingEvents.length > 0 ? (
              upcomingEvents.map((ev) => {
                const start = ev.allDay ? `${ev.extendedProps.dayKey}T00:00:00` : ev.start;

                return (
                  <div
                    key={ev.id}
                    onClick={() => {
                      setSelectedOccurrence({
                        occurrenceId: ev.extendedProps.occurrenceId,
                        scheduleId: ev.extendedProps.scheduleId,
                        calendarId: ev.extendedProps.calendarId,
                        title: ev.title,
                        theme: ev.extendedProps.theme,
                        startAtUtc: ev.extendedProps.startAtUtc,
                        endAtUtc: ev.extendedProps.endAtUtc,
                        recurring: ev.extendedProps.recurring,
                        allDay: ev.allDay,
                        occurrenceDate: ev.extendedProps.dayKey,
                        color: ev.extendedProps.color,
                      });
                      setSelectedDate(null);
                      setIsModalOpen(true);
                    }}
                    style={{
                      marginBottom: '0.5rem',
                      padding: '0.75rem',
                      backgroundColor: '#f9fafb',
                      borderLeft: `4px solid ${ev.extendedProps.color || '#3b82f6'}`,
                      borderRadius: '0.5rem',
                      cursor: 'pointer',
                    }}
                  >
                    <p style={{ fontWeight: '600', margin: 0 }}>{ev.title}</p>
                    <p style={{ fontSize: '0.75rem', color: '#6b7280', margin: 0 }}>
                      {formatDate(start)}
                    </p>
                  </div>
                );
              })
            ) : (
              <p style={{ color: '#6b7280', textAlign: 'center', padding: '1rem' }}>
                예정된 일정이 없습니다
              </p>
            )}
          </div>
        </div>
      </div>

      {/* 모달 */}
      {isModalOpen && (
        <ScheduleModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          selectedDate={selectedDate}
          eventData={selectedOccurrence}
        />
      )}
    </div>
  );
};
