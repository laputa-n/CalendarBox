import React, { useState } from 'react';
import { Calendar as CalendarIcon, Plus, Settings, Share2, Edit, Trash2 } from 'lucide-react';
import { useCalendars } from '../../contexts/CalendarContext';
import { generateCalendarColors } from '../../utils/colorUtils';
import { validateCalendar } from '../../utils/validationUtils';
import { LoadingSpinner } from '../common/LoadingSpinner';

export const CalendarPage = () => {
  const { 
    calendars, 
    createCalendar, 
    updateCalendar, 
    deleteCalendar, 
    shareCalendar,
    loading 
  } = useCalendars();
  
  const [showForm, setShowForm] = useState(false);
  const [editingCalendar, setEditingCalendar] = useState(null);
  const [showShareModal, setShowShareModal] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    color: '#3b82f6',
    isShared: false
  });

  const availableColors = generateCalendarColors();

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    const validation = validateCalendar(formData);
    if (!validation.isValid) {
      alert(Object.values(validation.errors).join('\n'));
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
      console.error('Failed to save calendar:', error);
    }
  };

  const resetForm = () => {
    setFormData({
      name: '',
      description: '',
      color: '#3b82f6',
      isShared: false
    });
    setEditingCalendar(null);
    setShowForm(false);
  };

  const handleEdit = (calendar) => {
    setFormData({
      name: calendar.name,
      description: calendar.description || '',
      color: calendar.color || '#3b82f6',
      isShared: calendar.isShared || false
    });
    setEditingCalendar(calendar);
    setShowForm(true);
  };

  const handleDelete = async (calendarId) => {
    if (window.confirm('정말로 이 캘린더를 삭제하시겠습니까? 모든 일정이 함께 삭제됩니다.')) {
      await deleteCalendar(calendarId);
    }
  };

  const handleShare = async (calendarId, shareData) => {
    try {
      await shareCalendar(calendarId, shareData);
      setShowShareModal(null);
    } catch (error) {
      console.error('Failed to share calendar:', error);
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
          <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>
            캘린더 관리
          </h1>
          <p style={{ color: '#6b7280' }}>
            캘린더를 생성하고 관리하세요
          </p>
        </div>
        <button 
          onClick={() => setShowForm(true)}
          style={buttonStyle}
          disabled={loading}
        >
          <Plus style={{ width: '1.25rem', height: '1.25rem' }} />
          캘린더 추가
        </button>
      </div>

      {/* 캘린더 추가/수정 폼 */}
      {showForm && (
        <div style={{ ...cardStyle, marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            {editingCalendar ? '캘린더 수정' : '새 캘린더 추가'}
          </h3>
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  캘린더 이름 *
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({...formData, name: e.target.value})}
                  required
                  maxLength={50}
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
                  색상
                </label>
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                  {availableColors.map(color => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => setFormData({...formData, color})}
                      style={{
                        width: '2rem',
                        height: '2rem',
                        backgroundColor: color,
                        border: formData.color === color ? '3px solid #1f2937' : '2px solid #e5e7eb',
                        borderRadius: '50%',
                        cursor: 'pointer',
                        padding: 0
                      }}
                      title={color}
                    />
                  ))}
                </div>
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

            <div style={{ marginBottom: '1rem' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={formData.isShared}
                  onChange={(e) => setFormData({...formData, isShared: e.target.checked})}
                  style={{ width: '1rem', height: '1rem' }}
                />
                <span style={{ fontSize: '0.875rem', fontWeight: '500' }}>
                  친구들과 공유
                </span>
              </label>
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
                {loading ? '저장 중...' : (editingCalendar ? '수정' : '추가')}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* 캘린더 목록 */}
      <div style={cardStyle}>
        {loading ? (
          <LoadingSpinner text="캘린더를 불러오는 중..." />
        ) : calendars.length > 0 ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '1rem' }}>
            {calendars.map(calendar => (
              <div key={calendar.id} style={{
                padding: '1.5rem',
                borderRadius: '0.75rem',
                backgroundColor: '#f8fafc',
                border: '1px solid #e2e8f0'
              }}>
                <div style={{ display: 'flex', alignItems: 'start', justifyContent: 'space-between', marginBottom: '1rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div style={{
                      width: '1.5rem',
                      height: '1.5rem',
                      backgroundColor: calendar.color || '#3b82f6',
                      borderRadius: '50%'
                    }} />
                    <h4 style={{ fontSize: '1.125rem', fontWeight: '600', color: '#1f2937', margin: 0 }}>
                      {calendar.name}
                    </h4>
                  </div>
                  
                  <div style={{ display: 'flex', gap: '0.25rem' }}>
                    {calendar.isShared && (
                      <button
                        onClick={() => setShowShareModal(calendar)}
                        style={{
                          padding: '0.5rem',
                          color: '#10b981',
                          backgroundColor: 'transparent',
                          border: 'none',
                          borderRadius: '0.25rem',
                          cursor: 'pointer'
                        }}
                        title="공유 설정"
                      >
                        <Share2 style={{ width: '1rem', height: '1rem' }} />
                      </button>
                    )}
                    <button
                      onClick={() => handleEdit(calendar)}
                      style={{
                        padding: '0.5rem',
                        color: '#2563eb',
                        backgroundColor: 'transparent',
                        border: 'none',
                        borderRadius: '0.25rem',
                        cursor: 'pointer'
                      }}
                      title="수정"
                    >
                      <Edit style={{ width: '1rem', height: '1rem' }} />
                    </button>
                    <button
                      onClick={() => handleDelete(calendar.id)}
                      style={{
                        padding: '0.5rem',
                        color: '#dc2626',
                        backgroundColor: 'transparent',
                        border: 'none',
                        borderRadius: '0.25rem',
                        cursor: 'pointer'
                      }}
                      title="삭제"
                    >
                      <Trash2 style={{ width: '1rem', height: '1rem' }} />
                    </button>
                  </div>
                </div>
                
                {calendar.description && (
                  <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: '0 0 1rem 0', lineHeight: '1.4' }}>
                    {calendar.description}
                  </p>
                )}
                
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', fontSize: '0.75rem', color: '#9ca3af' }}>
                  <span>생성일: {new Date(calendar.createdAt).toLocaleDateString('ko-KR')}</span>
                  {calendar.isShared && (
                    <span style={{ color: '#10b981', fontWeight: '500' }}>공유됨</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#6b7280' }}>
            <CalendarIcon style={{ width: '4rem', height: '4rem', color: '#d1d5db', margin: '0 auto 1rem auto' }} />
            <p style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>아직 캘린더가 없습니다.</p>
            <p style={{ color: '#9ca3af' }}>새로운 캘린더를 추가해서 일정을 관리해보세요!</p>
          </div>
        )}
      </div>

      {/* 공유 모달 */}
      {showShareModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000
        }}
        onClick={() => setShowShareModal(null)}>
          <div style={{
            backgroundColor: 'white',
            padding: '2rem',
            borderRadius: '1rem',
            maxWidth: '400px',
            width: '90%'
          }}
          onClick={(e) => e.stopPropagation()}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '1rem' }}>
              캘린더 공유
            </h3>
            <p style={{ color: '#6b7280', marginBottom: '1rem' }}>
              "{showShareModal.name}" 캘린더를 친구들과 공유하세요.
            </p>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                onClick={() => setShowShareModal(null)}
                style={{
                  flex: 1,
                  padding: '0.75rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '0.5rem',
                  backgroundColor: 'white',
                  color: '#374151',
                  cursor: 'pointer'
                }}
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};