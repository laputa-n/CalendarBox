// src/components/pages/CalendarPage.js
import React, { useState, useEffect, useRef } from 'react';
import { Calendar as CalendarIcon, Plus, Edit, Trash2 } from 'lucide-react';
import { useCalendars } from '../../contexts/CalendarContext';
import { useSchedules } from '../../contexts/ScheduleContext';
import { LoadingSpinner } from '../common/LoadingSpinner';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import { useParams , useNavigate } from 'react-router-dom';
import  ScheduleModal  from '../ScheduleModal/ScheduleModal';

export const CalendarPage = () => {
  const {
  calendars,
  createCalendar,
  updateCalendar,
  deleteCalendar,
  loading,
  setCurrentCalendar,
  currentCalendar,
  fetchOccurrences,
  occurrencesByDay
} = useCalendars();

  const { schedules, fetchSchedules, fetchAllSchedules } = useSchedules();
  const { calendarId } = useParams();
  const [showForm, setShowForm] = useState(false);
  const [editingCalendar, setEditingCalendar] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    type: 'PERSONAL',
    visibility: 'PRIVATE',
    isDefault: false,
  });

  const [selectedEvent, setSelectedEvent] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const navigate = useNavigate();
  const handleSelectCalendar = (calendar) => {
  navigate(`/calendar/${calendar.id}`);
};

// 1️⃣ URL → currentCalendar
useEffect(() => {
  if (!calendarId || calendars.length === 0) return;

  const id = Number(calendarId);
  const target = calendars.find((cal) => cal.id === id);

  if (target && currentCalendar?.id !== target.id) {
    setCurrentCalendar(target);
  }
}, [calendarId, calendars]);

// 2️⃣ currentCalendar → occurrences fetch
useEffect(() => {
  if (!currentCalendar) return;

  const api = calendarRef.current?.getApi();
  if (!api) return;

  const view = api.view;
  handleDatesSet({
    start: view.currentStart,
    end: view.currentEnd,
  });
}, [currentCalendar]);


  /** =========================
   *  캘린더 CRUD
   * ========================= */
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      alert('캘린더 이름을 입력해주세요.');
      return;
    }
    try {
      if (editingCalendar) {
        await updateCalendar(editingCalendar.id, formData);
      } else {
        await createCalendar(formData);
      }
      resetForm();
    } catch (error) {
      console.error('캘린더 저장 실패:', error);
    }
  };

  const resetForm = () => {
    setFormData({ name: '', type: 'PERSONAL', visibility: 'PRIVATE', isDefault: false });
    setEditingCalendar(null);
    setShowForm(false);
  };

  const handleEdit = (calendar) => {
    setFormData({
      name: calendar.name,
      type: calendar.type,
      visibility: calendar.visibility,
      isDefault: calendar.isDefault,
    });
    setEditingCalendar(calendar);
    setShowForm(true);
  };

  const handleDelete = async (calendarId) => {
    if (window.confirm('정말로 이 캘린더를 삭제하시겠습니까?')) {
      await deleteCalendar(calendarId);
    }
  };

  /** =========================
   *  일정 관련 이벤트 핸들러
   * ========================= */
  const handleDateClick = (info) => {
    if (!currentCalendar) {
      alert('먼저 캘린더를 선택해주세요.');
      return;
    }
    setSelectedDate(info.dateStr);
    setSelectedEvent(null);
    setIsModalOpen(true);
  };

  const handleEventClick = (info) => {
  const scheduleId = info.event.extendedProps.scheduleId;

  const event = schedules.find((s) => s.id === scheduleId);

  setSelectedEvent(event || null);
  setSelectedDate(null);
  setIsModalOpen(true);
};


  /** =========================
   *  스타일
   * ========================= */
  const cardStyle = {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb',
    marginBottom: '1.5rem',
  };

  const buttonStyle = {
    backgroundColor: '#2563eb',
    color: 'white',
    padding: '0.75rem 1rem',
    borderRadius: '0.5rem',
    border: 'none',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    fontSize: '0.875rem',
    fontWeight: '500',
  };

  const convertOccurrencesToEvents = () => {
  if (!occurrencesByDay) return [];

  const events = [];
  Object.entries(occurrencesByDay).forEach(([day, list]) => {
    console.log("🔵 occurrencesByDay:", occurrencesByDay);
    console.log("🔵 convertOccurrencesToEvents input:", occurrencesByDay);
    list.forEach((occ) => {
      // 시작 시각 우선순위: UTC > KST > 그냥 startAt
      const start =
        occ.startAtUtc ||
        occ.startAtKst ||
        occ.startAt;

      if (!start) return; // 시작이 없으면 패스

      events.push({
        id: occ.occurrenceId ?? occ.id,
        title: occ.title,
        start,           // ✅ 시작만 넘김 (end 안 씀)
        // end: 제거! (여기 때문에 막 길게 보였던 거)
        allDay: false,
        backgroundColor: getThemeColor(occ.theme),
        borderColor: getThemeColor(occ.theme),
        extendedProps: {
          scheduleId: occ.scheduleId,
          recurring: occ.recurring,
        },
      });
    });
  });

  console.log('✅ convertOccurrencesToEvents result:', events);
  return events;
};

const getThemeColor = (theme) => {
  const map = {
    BLUE: '#3b82f6',
    GREEN: '#10b981',
    ORANGE: '#f97316',
    RED: '#ef4444',
    PURPLE: '#8b5cf6',
  };
  return map[theme] || '#3b82f6';
};

const calendarRef = useRef(null);

const toKstIso = (date) => {
  const offset = 9 * 60 * 60 * 1000;
  return new Date(date.getTime() + offset).toISOString().replace('Z', '+09:00');
};

const handleDatesSet = (arg) => {
  if (!currentCalendar) return;

  fetchOccurrences({
    fromKst: toKstIso(arg.start),
    toKst: toKstIso(arg.end),
    calendarId: currentCalendar.id,
  });
};


  /** =========================
   *  렌더링
   * ========================= */
  return (
    <div>
      {/* 헤더 */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>캘린더 관리</h1>
          <p style={{ color: '#6b7280' }}>캘린더를 생성하고 일정을 관리하세요</p>
        </div>
        <button onClick={() => setShowForm(true)} style={buttonStyle} disabled={loading}>
          <Plus style={{ width: '1.25rem', height: '1.25rem' }} /> 새 캘린더
        </button>
      </div>

      {/* 캘린더 생성/수정 폼 */}
      {showForm && (
        <div style={cardStyle}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            {editingCalendar ? '캘린더 수정' : '새 캘린더 추가'}
          </h3>
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'grid', gap: '1rem', marginBottom: '1rem' }}>
              <div>
                <label style={{ fontWeight: '500', fontSize: '0.875rem' }}>캘린더 이름 *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  style={{ width: '100%', padding: '0.75rem', border: '1px solid #d1d5db', borderRadius: '0.5rem' }}
                />
              </div>

              <div>
                <label style={{ fontWeight: '500', fontSize: '0.875rem' }}>공개 범위</label>
                <select
                  value={formData.visibility}
                  onChange={(e) => setFormData({ ...formData, visibility: e.target.value })}
                  style={{ width: '100%', padding: '0.75rem', border: '1px solid #d1d5db', borderRadius: '0.5rem' }}
                >
                  <option value="PRIVATE">비공개</option>
                  <option value="PUBLIC">공개</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
              <button
                type="button"
                onClick={resetForm}
                style={{ padding: '0.75rem 1rem', border: '1px solid #d1d5db', borderRadius: '0.5rem' }}
              >
                취소
              </button>
              <button type="submit" style={buttonStyle}>
                {editingCalendar ? '수정' : '추가'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* ✅ 캘린더 목록 & 선택 */}
      <div style={cardStyle}>
        {loading ? (
          <LoadingSpinner text="캘린더를 불러오는 중..." />
        ) : calendars.length > 0 ? (
          calendars.map((calendar) => (
            <div
              key={calendar.id}
              style={{
                borderBottom: '1px solid #e5e7eb',
                padding: '1rem 0',
                backgroundColor: currentCalendar?.id === calendar.id ? '#eef2ff' : 'transparent',
                cursor: 'pointer',
              }}
              onClick={() => handleSelectCalendar(calendar)}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <strong>{calendar.name}</strong> ({calendar.visibility})
                  {calendar.isDefault && <span style={{ color: '#10b981', marginLeft: '0.5rem' }}>기본</span>}
                </div>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button onClick={() => handleEdit(calendar)}>
                    <Edit size={16} />
                  </button>
                  <button onClick={() => handleDelete(calendar.id)}>
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div style={{ textAlign: 'center', color: '#6b7280' }}>
            <CalendarIcon size={48} />
            <p>아직 캘린더가 없습니다.</p>
          </div>
        )}
      </div>

      {/* ✅ FullCalendar (선택된 캘린더 일정만 표시) */}
      {currentCalendar ? (
        <div style={cardStyle}>
          <FullCalendar
            ref={calendarRef}
            plugins={[dayGridPlugin, interactionPlugin]}
            initialView="dayGridMonth"
            locale="ko"
            events={convertOccurrencesToEvents()}
            datesSet={handleDatesSet}
            dateClick={handleDateClick}
            eventClick={handleEventClick}
            height="80vh"
            headerToolbar={{
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,dayGridWeek,dayGridDay',
  }}
/>

        </div>
      ) : (
        <div style={{ ...cardStyle, textAlign: 'center', color: '#6b7280' }}>
          <p>좌측에서 캘린더를 선택하면 일정이 표시됩니다.</p>
        </div>
      )}

      {/* ✅ 일정 생성/수정 모달 */}
      {isModalOpen && (
        <ScheduleModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          selectedDate={selectedDate}
          eventData={selectedEvent}
          selectedCalendar={currentCalendar} // ✅ 핵심: 선택된 캘린더 전달
        />
      )}
    </div>
  );
};
