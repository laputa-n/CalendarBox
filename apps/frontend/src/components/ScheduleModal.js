// src/components/ScheduleModal.js
import React, { useState, useEffect } from 'react';
import { Search, Plus, Trash2 } from 'lucide-react';
import { useSchedules } from '../contexts/ScheduleContext';

export const ScheduleModal = ({ isOpen, onClose, selectedDate, eventData }) => {
  const { createSchedule, updateSchedule } = useSchedules();

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    startDateTime: '',
    endDateTime: '',
    color: '#3b82f6',
    places: [],
    recurrence: null,
    todos: [],
    reminders: [],
  });

  const [newTodo, setNewTodo] = useState('');

  useEffect(() => {
    if (eventData) {
      setFormData({
        title: eventData.title || '',
        description: eventData.description || '',
        startDateTime: eventData.startDateTime || eventData.startAt,
        endDateTime: eventData.endDateTime || eventData.endAt,
        color: eventData.color || '#3b82f6',
        places: eventData.places || [],
        recurrence: eventData.recurrence || null,
        todos: eventData.todos || [],
        reminders: eventData.reminders || [],
      });
    } else if (selectedDate) {
      setFormData({
        title: '',
        description: '',
        startDateTime: `${selectedDate}T09:00`,
        endDateTime: `${selectedDate}T10:00`,
        color: '#3b82f6',
        places: [],
        recurrence: null,
        todos: [],
        reminders: [],
      });
    }
  }, [selectedDate, eventData]);

 // ---------------------------
// 일정 생성 / 수정
// ---------------------------
const handleSubmit = async (e) => {
  e.preventDefault();
  try {
    // ✅ 백엔드가 기대하는 필드 구조로 변환
    const transformedData = {
  ...formData,
  memo: formData.description,
  startAt: formData.startDateTime,
  endAt: formData.endDateTime,
  theme: 'BLUE',
  places: formData.places.map((p) => ({
    name: p,
    mode: 'MANUAL', // ✅ 추가 (백엔드 필수값)
  })),
  todos: formData.todos.map((t) => ({ content: t })),
};

    if (eventData) {
      await updateSchedule(eventData.id, transformedData);
    } else {
      await createSchedule(transformedData);
    }

    onClose();
  } catch (error) {
    console.error('일정 저장 실패:', error);
  }
};

  // ---------------------------
  // 하위 폼 기능
  // ---------------------------
  const handleAddTodo = () => {
    if (!newTodo.trim()) return;
    setFormData({ ...formData, todos: [...formData.todos, newTodo] });
    setNewTodo('');
  };

  const handleRemoveTodo = (index) => {
    setFormData({
      ...formData,
      todos: formData.todos.filter((_, i) => i !== index),
    });
  };

  const handleAddPlace = () => {
    const place = prompt('장소명을 입력해주세요.');
    if (place) {
      setFormData({ ...formData, places: [...formData.places, place] });
    }
  };

  const handleRemovePlace = (index) => {
    setFormData({
      ...formData,
      places: formData.places.filter((_, i) => i !== index),
    });
  };

  const handleReminderChange = (e) => {
    const value = e.target.value;
    setFormData({
      ...formData,
      reminders: value === 'none' ? [] : [value],
    });
  };

  if (!isOpen) return null;

  return (
    <div style={overlayStyle}>
      <div style={modalStyle}>
        <h2 style={{ marginBottom: '1rem' }}>
          {eventData ? '일정 수정' : '새 일정 추가'}
        </h2>
        <form onSubmit={handleSubmit}>
          {/* 제목 */}
          <input
            type="text"
            placeholder="일정 제목"
            value={formData.title}
            onChange={(e) =>
              setFormData({ ...formData, title: e.target.value })
            }
            required
            style={inputStyle}
          />

          {/* 시작 / 종료 */}
          <input
            type="datetime-local"
            value={formData.startDateTime}
            onChange={(e) =>
              setFormData({ ...formData, startDateTime: e.target.value })
            }
            style={inputStyle}
          />
          <input
            type="datetime-local"
            value={formData.endDateTime}
            onChange={(e) =>
              setFormData({ ...formData, endDateTime: e.target.value })
            }
            style={inputStyle}
          />

          {/* 장소 */}
          <div style={sectionStyle}>
            <label style={labelStyle}>📍 장소</label>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                type="button"
                onClick={handleAddPlace}
                style={subButton}
                title="장소 추가"
              >
                <Search size={16} />
              </button>
              <div style={{ flex: 1 }}>
                {formData.places.length > 0 ? (
                  formData.places.map((p, i) => (
                    <div
                      key={i}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        background: '#f9fafb',
                        borderRadius: '0.5rem',
                        padding: '0.25rem 0.5rem',
                        marginBottom: '0.25rem',
                      }}
                    >
                      <span>{p}</span>
                      <button
                        type="button"
                        onClick={() => handleRemovePlace(i)}
                        style={iconButton}
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  ))
                ) : (
                  <p style={{ color: '#9ca3af', fontSize: '0.875rem' }}>
                    장소 없음
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* 반복 */}
          <div style={sectionStyle}>
            <label style={labelStyle}>🔁 반복</label>
            <select
              value={formData.recurrence || 'none'}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  recurrence: e.target.value === 'none' ? null : e.target.value,
                })
              }
              style={inputStyle}
            >
              <option value="none">없음</option>
              <option value="DAILY">매일</option>
              <option value="WEEKLY">매주</option>
              <option value="MONTHLY">매월</option>
            </select>
          </div>

          {/* 투두리스트 */}
          <div style={sectionStyle}>
            <label style={labelStyle}>🧾 투두리스트</label>
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <input
                type="text"
                placeholder="할 일을 입력하세요"
                value={newTodo}
                onChange={(e) => setNewTodo(e.target.value)}
                style={{ ...inputStyle, flex: 1 }}
              />
              <button
                type="button"
                onClick={handleAddTodo}
                style={subButton}
              >
                <Plus size={16} />
              </button>
            </div>
            <ul style={{ margin: 0, paddingLeft: '1rem' }}>
              {formData.todos.map((todo, index) => (
                <li key={index} style={{ marginBottom: '0.25rem' }}>
                  {todo}
                  <button
                    type="button"
                    onClick={() => handleRemoveTodo(index)}
                    style={{ ...iconButton, marginLeft: '0.5rem' }}
                  >
                    <Trash2 size={14} />
                  </button>
                </li>
              ))}
            </ul>
          </div>

          {/* 리마인더 */}
          <div style={sectionStyle}>
            <label style={labelStyle}>⏰ 리마인더</label>
            <select
              value={formData.reminders[0] || 'none'}
              onChange={handleReminderChange}
              style={inputStyle}
            >
              <option value="none">없음</option>
              <option value="5m">5분 전</option>
              <option value="30m">30분 전</option>
              <option value="1h">1시간 전</option>
              <option value="1d">하루 전</option>
            </select>
          </div>

          {/* 색상 */}
          <div style={sectionStyle}>
            <label style={labelStyle}>🎨 색상</label>
            <input
              type="color"
              value={formData.color}
              onChange={(e) =>
                setFormData({ ...formData, color: e.target.value })
              }
              style={{ width: '100%', height: '2rem', border: 'none' }}
            />
          </div>

          {/* 설명 */}
          <textarea
            placeholder="메모 / 상세 내용"
            value={formData.description}
            onChange={(e) =>
              setFormData({ ...formData, description: e.target.value })
            }
            rows={3}
            style={{ ...inputStyle, height: '80px' }}
          />

          {/* 버튼 */}
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1rem' }}>
            <button type="button" onClick={onClose} style={cancelButton}>
              취소
            </button>
            <button type="submit" style={saveButton}>
              저장
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

// ✅ 스타일
const overlayStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  width: '100%',
  height: '100%',
  backgroundColor: 'rgba(0,0,0,0.5)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  zIndex: 1000,
};

const modalStyle = {
  backgroundColor: '#fff',
  padding: '2rem',
  borderRadius: '10px',
  width: '480px',
  maxWidth: '90%',
  maxHeight: '90vh',
  overflowY: 'auto',
};

const inputStyle = {
  width: '100%',
  marginBottom: '0.75rem',
  padding: '0.5rem',
  borderRadius: '0.5rem',
  border: '1px solid #d1d5db',
  fontSize: '0.875rem',
};

const sectionStyle = { marginBottom: '1rem' };
const labelStyle = { fontWeight: 500, fontSize: '0.875rem', marginBottom: '0.25rem', display: 'block' };

const subButton = {
  padding: '0.5rem',
  borderRadius: '0.5rem',
  background: '#e5e7eb',
  border: 'none',
  cursor: 'pointer',
};

const iconButton = {
  background: 'transparent',
  border: 'none',
  color: '#6b7280',
  cursor: 'pointer',
};

const saveButton = {
  backgroundColor: '#2563eb',
  color: '#fff',
  padding: '0.5rem 1.25rem',
  borderRadius: '0.5rem',
  border: 'none',
  cursor: 'pointer',
};

const cancelButton = {
  backgroundColor: '#e5e7eb',
  color: '#111',
  padding: '0.5rem 1.25rem',
  borderRadius: '0.5rem',
  border: 'none',
  cursor: 'pointer',
};
