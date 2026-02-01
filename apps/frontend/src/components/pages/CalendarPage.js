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
import { THEME_TO_COLOR } from '../../utils/colorUtils';

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
  setCurrentCalendar(calendar);          // ✅ 즉시 반영
  navigate(`/calendar/${calendar.id}`);  // URL도 동기화
};
// 1️⃣ URL → currentCalendar
useEffect(() => {
  if (!currentCalendar) return;
  const api = calendarRef.current?.getApi?.();
  if (!api) return;

  // 현재 캘린더가 보고 있는 범위
  const view = api.view;
  const from = toKstIso(view.activeStart);
  const to = toKstIso(view.activeEnd);

  // 중복 방지 key도 업데이트
  lastFetchKeyRef.current = `${currentCalendar.id}_${from}_${to}`;

  fetchOccurrences({
    fromKst: from,
    toKst: to,
    calendarId: currentCalendar.id,
  });

  // 필요하면 화면 강제 리렌더/갱신
  api.render();
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

const renderVisibility = (visibility) => {
  switch (visibility) {
    case 'PRIVATE':
      return '🔒 PRIVATE';
    case 'PROTECTED':
      return '🛡 PROTECTED';
    case 'PUBLIC':
      return '🌍 PUBLIC';
    default:
      return visibility;
  }
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

  console.log("🔵 convertOccurrencesToEvents input:", occurrencesByDay);

  const events = [];

  Object.entries(occurrencesByDay).forEach(([day, list]) => {
    list.forEach((occ) => {
      const start =
        occ.startAtUtc ||
        occ.startAtKst ||
        occ.startAt;

      if (!start) return;

      events.push({
        id: occ.occurrenceId ?? occ.id,
        title: occ.title,
        start,
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

  return events;
};

const getThemeColor = (theme) => {
  const map = {
    RED: '#ef4444',
    BLUE: '#3b82f6',
    GREEN: '#22c55e',
    YELLOW: '#eab308',
    PURPLE: '#a855f7',
    PINK: '#ec4899',
    BLACK: '#111827',
    ORANGE: '#f97316',
  };
  return map[theme] || '#3b82f6';
};


const calendarRef = useRef(null);

const events = React.useMemo(() => {
  if (!occurrencesByDay) return [];
  return convertOccurrencesToEvents();
}, [occurrencesByDay]);


const toKstIso = (date) => {
  const offset = 9 * 60 * 60 * 1000;
  return new Date(date.getTime() + offset).toISOString().replace('Z', '+09:00');
};

const lastFetchKeyRef = useRef('');

const handleDatesSet = (arg) => {
  if (!currentCalendar) return;

  const from = toKstIso(arg.start);
  const to = toKstIso(arg.end);
  const key = `${currentCalendar.id}_${from}_${to}`;

  // ✅ 동일한 조건이면 fetch 중단
  if (lastFetchKeyRef.current === key) {
    return;
  }

  lastFetchKeyRef.current = key;

  fetchOccurrences({
    fromKst: from,
    toKst: to,
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

        {/* 캘린더 이름 */}
        <div>
          <label style={{ fontWeight: '500', fontSize: '0.875rem' }}>
            캘린더 이름 *
          </label>
          <input
            type="text"
            value={formData.name}
            onChange={(e) =>
              setFormData({ ...formData, name: e.target.value })
            }
            required
            style={{
              width: '100%',
              padding: '0.75rem',
              border: '1px solid #d1d5db',
              borderRadius: '0.5rem',
            }}
          />
        </div>

        {/* 캘린더 타입 */}
        <div>
          <label style={{ fontWeight: '500', fontSize: '0.875rem' }}>
            캘린더 타입
          </label>
          <select
            value={formData.type}
            onChange={(e) =>
              setFormData({ ...formData, type: e.target.value })
            }
            style={{
              width: '100%',
              padding: '0.75rem',
              border: '1px solid #d1d5db',
              borderRadius: '0.5rem',
            }}
          >
            <option value="PERSONAL">개인 캘린더</option>
            <option value="GROUP">그룹 캘린더</option>
          </select>
        </div>

        {/* 공개 범위 */}
        <div>
          <label style={{ fontWeight: '500', fontSize: '0.875rem' }}>
            공개 범위
          </label>
          <select
            value={formData.visibility}
            onChange={(e) =>
              setFormData({ ...formData, visibility: e.target.value })
            }
            style={{
              width: '100%',
              padding: '0.75rem',
              border: '1px solid #d1d5db',
              borderRadius: '0.5rem',
            }}
          >
            <option value="PRIVATE">🔒 PRIVATE</option>
            <option value="PROTECTED"> PROTECTED</option>
            <option value="PUBLIC">🌍 PUBLIC</option>
          </select>

          {/* 선택 설명 */}
          <p style={{ fontSize: '0.75rem', color: '#6b7280', marginTop: 4 }}>
            {formData.visibility === 'PRIVATE' &&
              '본인만 사용하는 개인 캘린더입니다.'}
            {formData.visibility === 'PROTECTED' &&
              '공유되지만 설정 변경이 제한됩니다.'}
            {formData.visibility === 'PUBLIC' &&
              '누구나 접근 가능한 공개 캘린더입니다.'}
          </p>
        </div>

        {/* 기본 캘린더 설정 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <input
            type="checkbox"
            checked={formData.isDefault}
            onChange={(e) =>
              setFormData({ ...formData, isDefault: e.target.checked })
            }
          />
          <label style={{ fontSize: '0.875rem' }}>
            기본 캘린더로 설정
          </label>
        </div>
      </div>

      {/* 버튼 */}
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
        <button
          type="button"
          onClick={resetForm}
          style={{
            padding: '0.75rem 1rem',
            border: '1px solid #d1d5db',
            borderRadius: '0.5rem',
          }}
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
                 <strong>{calendar.name}</strong>
<span style={{ marginLeft: '0.5rem', fontSize: '0.875rem', color: '#6b7280' }}>
  {renderVisibility(calendar.visibility)}
</span>
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
           key={currentCalendar?.id}
            ref={calendarRef}
            plugins={[dayGridPlugin, interactionPlugin]}
            initialView="dayGridMonth"
            locale="ko"
            events={events}
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
