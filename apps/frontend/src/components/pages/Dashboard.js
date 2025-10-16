// src/components/pages/Dashboard.js
import React, { useEffect, useState } from 'react';
import {
  Calendar as CalendarIcon, Edit, Plus, Users, FileText, Bell
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useCalendars } from '../../contexts/CalendarContext';
import { useSchedules } from '../../contexts/ScheduleContext';
import { useFriends } from '../../contexts/FriendContext';
import { useNotifications } from '../../contexts/NotificationContext';
import { formatDate, formatTime, isToday } from '../../utils/dateUtils';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';



import { ScheduleModal } from '../ScheduleModal';
import { LoadingSpinner } from '../common/LoadingSpinner';

export const Dashboard = () => {
  const { user } = useAuth();
  const { calendars, refreshCalendars, loading } = useCalendars();
  const { schedules, fetchAllSchedules } = useSchedules();
  const { acceptedFriendships } = useFriends();
  const { unreadCount } = useNotifications();

  const [defaultCalendar, setDefaultCalendar] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedEvent, setSelectedEvent] = useState(null);

  // ✅ 최초 데이터 로드
  useEffect(() => {
    refreshCalendars();
    fetchAllSchedules();
  }, []);

  // ✅ 기본 캘린더 설정
  useEffect(() => {
    if (Array.isArray(calendars)) {
      const def = calendars.find(c => c.isDefault);
      setDefaultCalendar(def || null);
    }
  }, [calendars]);

  // ✅ 일정 CRUD 후 즉시 새로고침
  useEffect(() => {
    if (defaultCalendar) {
      fetchAllSchedules();
    }
  }, [defaultCalendar]);

  const handleDateClick = (info) => {
    setSelectedDate(info.dateStr);
    setSelectedEvent(null);
    setIsModalOpen(true);
  };

  const handleEventClick = (info) => {
    const event = schedules.find((s) => s.id === Number(info.event.id));
    setSelectedEvent(event);
    setSelectedDate(null);
    setIsModalOpen(true);
  };

  // 통계 계산
  const todaySchedules = schedules.filter(s => isToday(s.startDateTime));
  const upcomingSchedules = schedules
    .filter(s => new Date(s.startDateTime) > new Date())
    .sort((a, b) => new Date(a.startDateTime) - new Date(b.startDateTime))
    .slice(0, 5);

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

      {/* 통계 카드 섹션 */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '1rem',
          marginBottom: '2rem'
        }}
      >
        <div
          style={{
            ...cardStyle,
            background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
            color: 'white'
          }}
        >
          <p style={{ fontSize: '0.75rem' }}>오늘의 일정</p>
          <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{todaySchedules.length}</p>
        </div>

        <div
          style={{
            ...cardStyle,
            background: 'linear-gradient(135deg, #10b981 0%, #047857 100%)',
            color: 'white'
          }}
        >
          <p style={{ fontSize: '0.75rem' }}>친구 수</p>
          <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{acceptedFriendships.length}</p>
        </div>

        <div
          style={{
            ...cardStyle,
            background: 'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)',
            color: 'white'
          }}
        >
          <p style={{ fontSize: '0.75rem' }}>캘린더 수</p>
          <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{calendars.length}</p>
        </div>

        <div
          style={{
            ...cardStyle,
            background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
            color: 'white'
          }}
        >
          <p style={{ fontSize: '0.75rem' }}>읽지 않은 알림</p>
          <p style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>{unreadCount}</p>
        </div>
      </div>

      {/* 메인 콘텐츠 (캘린더 + 사이드 패널) */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
        {/* ✅ 캘린더 섹션 */}
        <div style={cardStyle}>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '1rem' }}>
            📅 내 기본 캘린더
          </h2>

          {!defaultCalendar ? (
            <div style={{ textAlign: 'center', color: '#6b7280', padding: '3rem' }}>
              <CalendarIcon size={48} style={{ marginBottom: '1rem' }} />
              <p>기본 캘린더가 없습니다.</p>
              <p>‘캘린더 관리’에서 기본 캘린더를 생성해주세요.</p>
            </div>
          ) : (
            <FullCalendar
              plugins={[dayGridPlugin, interactionPlugin]}
              initialView="dayGridMonth"
              locale="ko"
              height="80vh"
              events={schedules
                .filter((s) => s.calendarId === defaultCalendar.calendarId)
                .map((s) => ({
                  id: s.id,
                  title: s.title,
                  start: s.startAt || s.startDateTime,
                  end: s.endAt || s.endDateTime,
                  backgroundColor: s.color || '#3b82f6',
                  borderColor: s.color || '#3b82f6',
                }))
              }
              dateClick={handleDateClick}
              eventClick={handleEventClick}
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,dayGridWeek,dayGridDay',
              }}
            />
          )}
        </div>

        {/* ✅ 오른쪽 사이드 패널 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* 오늘의 일정 */}
          <div style={cardStyle}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
              오늘의 일정
            </h3>
            {todaySchedules.length > 0 ? (
              todaySchedules.map((s) => (
                <div
                  key={s.id}
                  onClick={() => handleEventClick({ event: { id: s.id } })}
                  style={{
                    marginBottom: '0.5rem',
                    padding: '0.75rem',
                    backgroundColor: '#f9fafb',
                    borderLeft: `4px solid ${s.color || '#3b82f6'}`,
                    borderRadius: '0.5rem',
                    cursor: 'pointer',
                  }}
                >
                  <p style={{ fontWeight: '600', margin: 0 }}>{s.title}</p>
                  <p style={{ fontSize: '0.75rem', color: '#6b7280', margin: 0 }}>
                    {formatTime(s.startDateTime)} ~ {formatTime(s.endDateTime)}
                  </p>
                </div>
              ))
            ) : (
              <p style={{ color: '#6b7280', textAlign: 'center', padding: '1rem' }}>
                오늘 일정이 없습니다
              </p>
            )}
          </div>

          {/* 다가오는 일정 */}
          <div style={cardStyle}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
              다가오는 일정
            </h3>
            {upcomingSchedules.length > 0 ? (
              upcomingSchedules.map((s) => (
                <div
                  key={s.id}
                  onClick={() => handleEventClick({ event: { id: s.id } })}
                  style={{
                    marginBottom: '0.5rem',
                    padding: '0.75rem',
                    backgroundColor: '#f9fafb',
                    borderLeft: `4px solid ${s.color || '#3b82f6'}`,
                    borderRadius: '0.5rem',
                    cursor: 'pointer',
                  }}
                >
                  <p style={{ fontWeight: '600', margin: 0 }}>{s.title}</p>
                  <p style={{ fontSize: '0.75rem', color: '#6b7280', margin: 0 }}>
                    {formatDate(s.startDateTime)}
                  </p>
                </div>
              ))
            ) : (
              <p style={{ color: '#6b7280', textAlign: 'center', padding: '1rem' }}>
                예정된 일정이 없습니다
              </p>
            )}
          </div>
        </div>
      </div>

      {/* ✅ 일정 모달 */}
      {isModalOpen && (
        <ScheduleModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          selectedDate={selectedDate}
          eventData={selectedEvent}
        />
      )}
    </div>
  );
};
