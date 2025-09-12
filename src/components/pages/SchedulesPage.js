import React, { useState } from 'react';
import { Plus, Edit, Trash2, Clock, MapPin, Loader2, Calendar } from 'lucide-react';
import { useSchedules } from '../../contexts/ScheduleContext';
import { formatDateTime } from '../../utils/dateUtils';
import { validateSchedule } from '../../utils/validationUtils';

export const SchedulesPage = () => {
  const { schedules, createSchedule, updateSchedule, deleteSchedule, loading } = useSchedules();
  const [showForm, setShowForm] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    startDateTime: '',
    endDateTime: '',
    isAllDay: false,
    location: '',
    color: '#3b82f6',
    visibility: 'public',
    status: 'confirmed'
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const validation = validateSchedule(formData);
    if (!validation.isValid) {
      alert(Object.values(validation.errors).join('\n'));
      return;
    }

    try {
      if (editingSchedule) {
        await updateSchedule(editingSchedule.id, formData);
      } else {
        await createSchedule(formData);
      }
      resetForm();
    } catch (error) {
      console.error('Failed to save schedule:', error);
    }
  };

  const resetForm = () => {
    setFormData({
      title: '',
      description: '',
      startDateTime: '',
      endDateTime: '',
      isAllDay: false,
      location: '',
      color: '#3b82f6',
      visibility: 'public',
      status: 'confirmed'
    });
    setEditingSchedule(null);
    setShowForm(false);
  };

  const handleEdit = (schedule) => {
    setFormData({
      title: schedule.title,
      description: schedule.description || '',
      startDateTime: schedule.startDateTime ? schedule.startDateTime.slice(0, 16) : '',
      endDateTime: schedule.endDateTime ? schedule.endDateTime.slice(0, 16) : '',
      isAllDay: schedule.isAllDay || false,
      location: schedule.location || '',
      color: schedule.color || '#3b82f6',
      visibility: schedule.visibility || 'public',
      status: schedule.status || 'confirmed'
    });
    setEditingSchedule(schedule);
    setShowForm(true);
  };

  const handleDelete = async (scheduleId) => {
    if (window.confirm('정말로 삭제하시겠습니까?')) {
      await deleteSchedule(scheduleId);
    }
  };

  const cardStyle = {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb'
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
    fontWeight: '500'
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>일정 관리</h1>
          <p style={{ color: '#6b7280' }}>모든 일정을 한곳에서 관리하세요</p>
        </div>
        <button 
          onClick={() => setShowForm(true)} 
          style={buttonStyle}
          disabled={loading}
        >
          <Plus style={{ width: '1.25rem', height: '1.25rem' }} />
          일정 추가
        </button>
      </div>

      {/* 일정 추가/수정 폼 */}
      {showForm && (
        <div style={{ ...cardStyle, marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            {editingSchedule ? '일정 수정' : '새 일정 추가'}
          </h3>
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  제목 *
                </label>
                <input
                  type="text"
                  value={formData.title}
                  onChange={(e) => setFormData({...formData, title: e.target.value})}
                  required
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  장소
                </label>
                <input
                  type="text"
                  value={formData.location}
                  onChange={(e) => setFormData({...formData, location: e.target.value})}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  시작 시간 *
                </label>
                <input
                  type="datetime-local"
                  value={formData.startDateTime}
                  onChange={(e) => setFormData({...formData, startDateTime: e.target.value})}
                  required={!formData.isAllDay}
                  disabled={formData.isAllDay}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                    fontSize: '0.875rem',
                    opacity: formData.isAllDay ? 0.5 : 1
                  }}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  종료 시간 *
                </label>
                <input
                  type="datetime-local"
                  value={formData.endDateTime}
                  onChange={(e) => setFormData({...formData, endDateTime: e.target.value})}
                  required={!formData.isAllDay}
                  disabled={formData.isAllDay}
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                    fontSize: '0.875rem',
                    opacity: formData.isAllDay ? 0.5 : 1
                  }}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  색상
                </label>
                <input
                  type="color"
                  value={formData.color}
                  onChange={(e) => setFormData({...formData, color: e.target.value})}
                  style={{
                    width: '100%',
                    height: '2.75rem',
                    padding: '0.25rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem'
                  }}
                />
              </div>
            </div>

            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                설명
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({...formData, description: e.target.value})}
                rows={3}
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '0.5rem',
                  fontSize: '0.875rem',
                  resize: 'vertical'
                }}
              />
            </div>

            <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
              <button
                type="button"
                onClick={resetForm}
                style={{
                  padding: '0.75rem 1rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '0.5rem',
                  backgroundColor: 'white',
                  color: '#374151',
                  cursor: 'pointer'
                }}
              >
                취소
              </button>
              <button
                type="submit"
                disabled={loading}
                style={{
                  ...buttonStyle,
                  opacity: loading ? 0.6 : 1,
                  cursor: loading ? 'not-allowed' : 'pointer'
                }}
              >
                {loading ? 
                  <Loader2 style={{ width: '1rem', height: '1rem', animation: 'spin 1s linear infinite' }} /> : 
                  (editingSchedule ? '수정' : '추가')
                }
              </button>
            </div>
          </form>
        </div>
      )}

      {/* 일정 목록 */}
      <div style={cardStyle}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '2rem' }}>
            <Loader2 style={{ width: '2rem', height: '2rem', animation: 'spin 1s linear infinite', color: '#2563eb' }} />
            <p style={{ color: '#6b7280', marginTop: '1rem' }}>일정을 불러오는 중...</p>
          </div>
        ) : schedules.length > 0 ? (
          schedules.map((schedule, index) => (
            <div key={schedule.id} style={{
              padding: '1.5rem',
              borderBottom: index < schedules.length - 1 ? '1px solid #e5e7eb' : 'none'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                    <div style={{
                      width: '1rem',
                      height: '1rem',
                      backgroundColor: schedule.color || '#3b82f6',
                      borderRadius: '50%'
                    }} />
                    <h3 style={{ fontSize: '1.125rem', fontWeight: '600', color: '#1f2937', margin: 0 }}>
                      {schedule.title}
                    </h3>
                  </div>
                  
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', fontSize: '0.875rem', color: '#6b7280' }}>
                      <Clock style={{ width: '1rem', height: '1rem', marginRight: '0.5rem' }} />
                      <span>
                        {schedule.isAllDay ? '하루 종일' : 
                          `${formatDateTime(schedule.startDateTime)} - ${formatDateTime(schedule.endDateTime)}`
                        }
                      </span>
                    </div>
                    {schedule.location && (
                      <div style={{ display: 'flex', alignItems: 'center', fontSize: '0.875rem', color: '#6b7280' }}>
                        <MapPin style={{ width: '1rem', height: '1rem', marginRight: '0.5rem' }} />
                        <span>{schedule.location}</span>
                      </div>
                    )}
                    {schedule.description && (
                      <p style={{ fontSize: '0.875rem', color: '#6b7280', marginTop: '0.5rem', margin: 0 }}>
                        {schedule.description}
                      </p>
                    )}
                  </div>
                </div>
                
                <div style={{ display: 'flex', gap: '0.5rem', marginLeft: '1rem' }}>
                  <button
                    onClick={() => handleEdit(schedule)}
                    style={{
                      padding: '0.5rem',
                      color: '#2563eb',
                      backgroundColor: 'transparent',
                      border: 'none',
                      borderRadius: '0.5rem',
                      cursor: 'pointer'
                    }}
                  >
                    <Edit style={{ width: '1rem', height: '1rem' }} />
                  </button>
                  <button
                    onClick={() => handleDelete(schedule.id)}
                    style={{
                      padding: '0.5rem',
                      color: '#dc2626',
                      backgroundColor: 'transparent',
                      border: 'none',
                      borderRadius: '0.5rem',
                      cursor: 'pointer'
                    }}
                  >
                    <Trash2 style={{ width: '1rem', height: '1rem' }} />
                  </button>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#6b7280' }}>
            <Calendar style={{ width: '4rem', height: '4rem', color: '#d1d5db', margin: '0 auto 1rem auto' }} />
            <p style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>등록된 일정이 없습니다.</p>
            <p style={{ color: '#9ca3af' }}>새로운 일정을 추가해보세요!</p>
          </div>
        )}
      </div>
    </div>
  );
};