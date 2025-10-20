// src/pages/CalendarBoardPage.js
import React, { useEffect, useState } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { useSchedules } from '../../contexts/ScheduleContext';
import { useCalendars } from '../../contexts/CalendarContext';
import { ScheduleModal } from '../ScheduleModal';

export const CalendarBoardPage = () => {
  const { fetchSchedules, schedules } = useSchedules();
  const { calendars } = useCalendars(); // ✅ 모든 캘린더 가져오기
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);

  // ✅ 각 캘린더별 일정 가져오기
  useEffect(() => {
    if (calendars.length > 0) {
      calendars.forEach((calendar) => fetchSchedules({ calendarId: calendar.id }));
    }
  }, [calendars]);

  const handleDateClick = (info, calendarId) => {
    setSelectedDate(info.dateStr);
    setSelectedEvent(null);
    setIsModalOpen(true);
  };

  const handleEventClick = (info, calendarId) => {
    const event = schedules.find((s) => s.id === Number(info.event.id));
    setSelectedEvent(event);
    setSelectedDate(null);
    setIsModalOpen(true);
  };

  return (
    <div style={{ padding: '2rem', backgroundColor: '#f9fafb', borderRadius: '1rem' }}>
      <h1 style={{ fontSize: '1.75rem', fontWeight: '700', marginBottom: '1rem' }}>
        내 캘린더
      </h1>

      {calendars.length === 0 ? (
        <p>생성된 캘린더가 없습니다. 캘린더를 먼저 추가하세요.</p>
      ) : (
        calendars.map((calendar) => (
          <div
            key={calendar.id}
            style={{
              marginBottom: '3rem',
              backgroundColor: '#fff',
              borderRadius: '0.75rem',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
              padding: '1.5rem',
            }}
          >
            <h2 style={{ fontSize: '1.25rem', fontWeight: '600', marginBottom: '1rem' }}>
              📅 {calendar.name}
            </h2>

            <FullCalendar
              plugins={[dayGridPlugin, interactionPlugin]}
              initialView="dayGridMonth"
              locale="ko"
              events={schedules
                .filter((s) => s.calendarId === calendar.id)
                .map((s) => ({
                  id: s.id,
                  title: s.title,
                  start: s.startAt || s.startDateTime,
                  end: s.endAt || s.endDateTime,
                  backgroundColor: s.color,
                  borderColor: s.color,
                }))}
              dateClick={(info) => handleDateClick(info, calendar.id)}
              eventClick={(info) => handleEventClick(info, calendar.id)}
              height="60vh"
              headerToolbar={{
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,dayGridWeek,dayGridDay',
              }}
            />
          </div>
        ))
      )}

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
