// src/pages/SchedulesPage.js
import React, { useEffect, useState, useCallback } from 'react';
import {
  Plus,
  Edit,
  Trash2,
  Clock,
  MapPin,
  Loader2,
  Calendar,
  Check,
  X,
} from 'lucide-react';

import { useSchedules } from '../../contexts/ScheduleContext';
import { useCalendars } from '../../contexts/CalendarContext';
import { formatDateTime } from '../../utils/dateUtils';
import { validateSchedule } from '../../utils/validationUtils';
import { ScheduleDetailModal } from './ScheduleDetailModal';
import { ApiService } from '../../services/apiService';

export const SchedulesPage = () => {
  const {
    schedules,
    createSchedule,
    updateSchedule,
    deleteSchedule,
    loading,
    fetchSchedules,
    searchSchedules,
  } = useSchedules();

  const { calendars, currentCalendar, setCurrentCalendar } = useCalendars();

  const [showForm, setShowForm] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState(null);
  const [selectedSchedule, setSelectedSchedule] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  // ✅ 받은 초대 목록 상태
  const [invited, setInvited] = useState([]);
  const [invitedLoading, setInvitedLoading] = useState(false);
  const [invitedPage, setInvitedPage] = useState({
    page: 0,
    size: 20,
    totalPages: 0,
    totalElements: 0,
  });
  const [respondingId, setRespondingId] = useState(null);

  // 기본 폼 상태
  const initialFormState = {
    title: '',
    description: '',
    startDateTime: '',
    endDateTime: '',
    isAllDay: false,
    location: '',
    color: '#3b82f6',
  };
  const [formData, setFormData] = useState(initialFormState);

  /** =============================
   *  ✅ 받은 초대 목록 조회
   * ============================= */
  const fetchInvited = useCallback(
    async (page = 0) => {
      try {
        setInvitedLoading(true);

        // GET /api/schedules/invited?page=&size=
        const res = await ApiService.getInvitedSchedules(page, invitedPage.size);

        // axios response일 수도 / payload만 리턴일 수도 있으니 언랩
        const payload = res?.data ?? res;
        const data = payload?.data ?? payload;

        const content = data?.content ?? [];
        setInvited(Array.isArray(content) ? content : []);

        setInvitedPage({
          page: data?.page ?? page,
          size: data?.size ?? invitedPage.size,
          totalPages: data?.totalPages ?? 0,
          totalElements: data?.totalElements ?? 0,
        });
      } catch (e) {
        console.error('[getInvitedSchedules] failed', e);
        setInvited([]);
      } finally {
        setInvitedLoading(false);
      }
    },
    [invitedPage.size]
  );

  /** =============================
   *  ✅ 수락/거절
   * ============================= */
  const handleRespondInvite = async (inv, action) => {
    try {
      setRespondingId(inv.scheduleParticipantId);

    await ApiService.respondToScheduleInvite(
  inv.scheduleId,
  inv.scheduleParticipantId,
  action
);

      // 화면에서 제거
      setInvited((prev) =>
        (prev || []).filter((x) => x.scheduleParticipantId !== inv.scheduleParticipantId)
      );

      // ✅ 수락이면 일정 목록에 반영될 수 있으므로 갱신
      if (action === 'ACCEPT') {
        await fetchSchedules();
      }
    } catch (e) {
      console.error('[respondScheduleInvite] failed', e);
      alert('초대 응답 실패');
    } finally {
      setRespondingId(null);
    }
  };

  /** =============================
   *  캘린더 변경 시: 초대 목록도 갱신
   * ============================= */
  useEffect(() => {
    if (!currentCalendar?.id) {
      setInvited([]);
      return;
    }
    fetchInvited(0);
  }, [currentCalendar?.id, fetchInvited]);

  /** =============================
   *  폼 제출
   * ============================= */
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!currentCalendar) {
      alert('캘린더를 먼저 선택해주세요.');
      return;
    }

    const validation = validateSchedule(formData);
    if (!validation.isValid) {
      alert(Object.values(validation.errors).join('\n'));
      return;
    }

    try {
      if (editingSchedule) {
        await updateSchedule(editingSchedule.id, {
          ...formData,
          startAt: formData.startDateTime,
          endAt: formData.endDateTime,
          memo: formData.description,
          // updateSchedule는 color를 보고 theme 매핑함(컨텍스트 구현 기준)
          color: formData.color,
        });
      } else {
        await createSchedule({
          ...formData,
          startAt: formData.startDateTime,
          endAt: formData.endDateTime,
          memo: formData.description,
          // createSchedule도 color를 보고 theme 매핑함(컨텍스트 구현 기준)
          color: formData.color,
        });
      }
      resetForm();
    } catch (error) {
      console.error('Failed to save schedule:', error);
    }
  };

  /** =============================
   *  일정 검색
   * ============================= */
  const handleSearch = () => {
    if (!searchQuery.trim()) {
      fetchSchedules();
      return;
    }
    // (주의) Context의 searchSchedules 시그니처에 맞게 유지
    searchSchedules(searchQuery);
  };

  /** =============================
   *  폼 초기화
   * ============================= */
  const resetForm = () => {
    setFormData(initialFormState);
    setEditingSchedule(null);
    setShowForm(false);
  };

  /** =============================
   *  일정 수정
   * ============================= */
  const handleEdit = (schedule) => {
    setFormData({
      title: schedule.title,
      description: schedule.memo || schedule.description || '',
      startDateTime: schedule.startDateTime
        ? schedule.startDateTime.slice(0, 16)
        : schedule.startAt
        ? schedule.startAt.slice(0, 16)
        : '',
      endDateTime: schedule.endDateTime
        ? schedule.endDateTime.slice(0, 16)
        : schedule.endAt
        ? schedule.endAt.slice(0, 16)
        : '',
      isAllDay: schedule.isAllDay || false,
      location: schedule.location || '',
      color: schedule.color || '#3b82f6',
    });
    setEditingSchedule(schedule);
    setShowForm(true);
  };

  /** =============================
   *  일정 삭제
   * ============================= */
  const handleDelete = async (scheduleId) => {
    if (window.confirm('정말로 삭제하시겠습니까?')) {
      await deleteSchedule(scheduleId);
    }
  };

  /** =============================
   *  스타일 정의
   * ============================= */
  const cardStyle = {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb',
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

  return (
    <div>
      {/* 상단 헤더 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '1.5rem',
        }}
      >
        <div>
          <h1
            style={{
              fontSize: '1.875rem',
              fontWeight: 'bold',
              color: '#1f2937',
              marginBottom: '0.5rem',
            }}
          >
            일정 관리
          </h1>
          <p style={{ color: '#6b7280' }}>
            {currentCalendar
              ? `${currentCalendar.name} 캘린더의 일정을 관리하세요`
              : '캘린더를 먼저 선택해주세요'}
          </p>
        </div>

        {/* 🔍 일정 검색 + 캘린더 선택 */}
        <div
          style={{
            marginBottom: '1.5rem',
            display: 'flex',
            gap: '0.5rem',
            alignItems: 'center',
          }}
        >
          {/* 📅 캘린더 선택 */}
          <select
            value={currentCalendar?.id || ''}
            onChange={(e) => {
              const next = calendars.find((c) => String(c.id) === e.target.value);
              if (next) setCurrentCalendar(next);
            }}
            style={{
              padding: '0.5rem 0.75rem',
              border: '1px solid #d1d5db',
              borderRadius: '0.5rem',
              fontSize: '0.875rem',
              minWidth: '200px',
              backgroundColor: 'white',
            }}
          >
            <option value="" disabled>
              캘린더 선택
            </option>
            {calendars.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
                {c.isDefault ? ' (기본)' : ''}
              </option>
            ))}
          </select>

          {/* 🔍 검색 입력 */}
          <input
            type="text"
            placeholder="일정 검색(제목, 메모, 참가자, 장소 ...)"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch();
            }}
            style={{
              flex: 1,
              padding: '0.5rem 0.75rem',
              border: '1px solid #d1d5db',
              borderRadius: '0.5rem',
              fontSize: '0.875rem',
            }}
          />

          {/* 검색 버튼 */}
          <button
            onClick={handleSearch}
            disabled={loading}
            style={{
              padding: '0.5rem 1rem',
              backgroundColor: '#2563eb',
              color: '#fff',
              border: 'none',
              borderRadius: '0.5rem',
              fontSize: '0.875rem',
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
          >
            검색
          </button>
        </div>
      </div>

      {/* ✅ 받은 초대 목록: 캘린더 선택 시 일정 리스트 위에 표시 */}
      {currentCalendar && (
        <div style={{ ...cardStyle, marginBottom: '1rem' }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: 10,
            }}
          >
            <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>
              받은 일정 초대
            </h3>

            <button
              type="button"
              onClick={() => fetchInvited(0)}
              style={{
                padding: '6px 10px',
                borderRadius: 8,
                border: '1px solid #e5e7eb',
                background: '#fff',
                cursor: 'pointer',
              }}
              disabled={invitedLoading}
            >
              새로고침
            </button>
          </div>

          {invitedLoading ? (
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', color: '#6b7280' }}>
              <Loader2 style={{ width: 16, height: 16, animation: 'spin 1s linear infinite' }} />
              불러오는 중...
            </div>
          ) : invited.length === 0 ? (
            <div style={{ color: '#6b7280', fontSize: 14 }}>받은 초대가 없습니다.</div>
          ) : (
            <>
              {invited.map((inv) => (
                <div
                  key={inv.scheduleParticipantId}
                  style={{
                    border: '1px solid #e5e7eb',
                    borderRadius: 10,
                    padding: 12,
                    marginBottom: 8,
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 12,
                    background: '#fafafa',
                  }}
                >
                  <div style={{ minWidth: 0 }}>
                    <div
                      style={{
                        fontWeight: 700,
                        marginBottom: 4,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {inv.scheduleTitle}
                    </div>

                   <div style={{ fontSize: 13, color: '#6b7280' }}>
  초대자: {inv.inviterName || '알 수 없음'}
</div>

<div style={{ fontSize: 13, color: '#6b7280', marginTop: 4 }}>
  {formatDateTime(inv.startAt)} - {formatDateTime(inv.endAt)}
</div>

{/* ✅ 초대시간 표시 (필드 후보들 중 있는 것 사용) */}
<div style={{ fontSize: 12, color: '#9ca3af', marginTop: 4 }}>
  초대시간: {formatDateTime(inv.invitedAt || inv.createdAt || inv.respondedAt)}
</div>
                    <button
                      type="button"
                      onClick={() => setSelectedSchedule(inv.scheduleId)}
                      style={{
                        marginTop: 8,
                        padding: '6px 10px',
                        borderRadius: 8,
                        border: '1px solid #e5e7eb',
                        background: '#fff',
                        cursor: 'pointer',
                        fontSize: 12,
                      }}
                    >
                      상세 보기
                    </button>
                  </div>

                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <button
                      type="button"
                      onClick={() => handleRespondInvite(inv, 'ACCEPT')}
                      disabled={respondingId === inv.scheduleParticipantId}
                      style={{
                        padding: '8px 10px',
                        borderRadius: 8,
                        border: 'none',
                        cursor: 'pointer',
                        background: '#2563eb',
                        color: '#fff',
                        opacity: respondingId === inv.scheduleParticipantId ? 0.6 : 1,
                      }}
                      title="수락"
                    >
                      <Check size={16} />
                    </button>

                    <button
                      type="button"
                      onClick={() => handleRespondInvite(inv, 'REJECT')}
                      disabled={respondingId === inv.scheduleParticipantId}
                      style={{
                        padding: '8px 10px',
                        borderRadius: 8,
                        border: 'none',
                        cursor: 'pointer',
                        background: '#ef4444',
                        color: '#fff',
                        opacity: respondingId === inv.scheduleParticipantId ? 0.6 : 1,
                      }}
                      title="거절"
                    >
                      <X size={16} />
                    </button>
                  </div>
                </div>
              ))}

              {/* ✅ 간단 페이징 */}
              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
                <button
                  type="button"
                  onClick={() => fetchInvited(Math.max(0, invitedPage.page - 1))}
                  disabled={invitedPage.page <= 0 || invitedLoading}
                  style={{
                    padding: '6px 10px',
                    borderRadius: 8,
                    border: '1px solid #e5e7eb',
                    background: '#fff',
                    cursor: 'pointer',
                  }}
                >
                  이전
                </button>

                <div style={{ fontSize: 12, color: '#6b7280' }}>
                  {invitedPage.page + 1} / {Math.max(1, invitedPage.totalPages)}
                </div>

                <button
                  type="button"
                  onClick={() => fetchInvited(invitedPage.page + 1)}
                  disabled={invitedPage.page + 1 >= invitedPage.totalPages || invitedLoading}
                  style={{
                    padding: '6px 10px',
                    borderRadius: 8,
                    border: '1px solid #e5e7eb',
                    background: '#fff',
                    cursor: 'pointer',
                  }}
                >
                  다음
                </button>
              </div>
            </>
          )}
        </div>
      )}

      {/* 일정 추가/수정 폼 */}
      {showForm && (
        <div style={{ ...cardStyle, marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            {editingSchedule ? '일정 수정' : '새 일정 추가'}
          </h3>
          <form onSubmit={handleSubmit}>
            {/* 제목 / 장소 */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
                gap: '1rem',
                marginBottom: '1rem',
              }}
            >
              <div>
                <label className="label">제목 *</label>
                <input
                  type="text"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  required
                  className="input"
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                  }}
                />
              </div>
              <div>
                <label className="label">장소</label>
                <input
                  type="text"
                  value={formData.location}
                  onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                  className="input"
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                  }}
                />
              </div>
            </div>

            {/* 시간 / 색상 */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '1rem',
                marginBottom: '1rem',
              }}
            >
              <div>
                <label className="label">시작 시간 *</label>
                <input
                  type="datetime-local"
                  value={formData.startDateTime}
                  onChange={(e) => setFormData({ ...formData, startDateTime: e.target.value })}
                  required
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                  }}
                />
              </div>
              <div>
                <label className="label">종료 시간 *</label>
                <input
                  type="datetime-local"
                  value={formData.endDateTime}
                  onChange={(e) => setFormData({ ...formData, endDateTime: e.target.value })}
                  required
                  style={{
                    width: '100%',
                    padding: '0.75rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                  }}
                />
              </div>
              <div>
                <label className="label">색상</label>
                <input
                  type="color"
                  value={formData.color}
                  onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                  style={{
                    width: '100%',
                    height: '2.75rem',
                    padding: '0.25rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                  }}
                />
              </div>
            </div>

            {/* 설명 */}
            <div style={{ marginBottom: '1rem' }}>
              <label className="label">설명</label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                rows={3}
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: '1px solid #d1d5db',
                  borderRadius: '0.5rem',
                  resize: 'vertical',
                }}
              />
            </div>

            {/* 버튼 */}
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
                  cursor: 'pointer',
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
                  cursor: loading ? 'not-allowed' : 'pointer',
                }}
              >
                {loading ? (
                  <Loader2
                    style={{
                      width: '1rem',
                      height: '1rem',
                      animation: 'spin 1s linear infinite',
                    }}
                  />
                ) : editingSchedule ? (
                  '수정'
                ) : (
                  '추가'
                )}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* 일정 리스트 */}
      <div style={cardStyle}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '2rem' }}>
            <Loader2
              style={{
                width: '2rem',
                height: '2rem',
                animation: 'spin 1s linear infinite',
                color: '#2563eb',
              }}
            />
            <p style={{ color: '#6b7280', marginTop: '1rem' }}>일정을 불러오는 중...</p>
          </div>
        ) : schedules.length > 0 ? (
          schedules.map((schedule, index) => (
            <div
              key={schedule.id}
              style={{
                padding: '1.5rem',
                borderBottom: index < schedules.length - 1 ? '1px solid #e5e7eb' : 'none',
                cursor: 'pointer',
              }}
              onClick={() => setSelectedSchedule(schedule.id)}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'start',
                }}
              >
                <div style={{ flex: 1 }}>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      marginBottom: '0.5rem',
                    }}
                  >
                    <div
                      style={{
                        width: '1rem',
                        height: '1rem',
                        backgroundColor: schedule.color || '#3b82f6',
                        borderRadius: '50%',
                      }}
                    />
                    <h3
                      style={{
                        fontSize: '1.125rem',
                        fontWeight: '600',
                        color: '#1f2937',
                        margin: 0,
                      }}
                    >
                      {schedule.title}
                    </h3>
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        fontSize: '0.875rem',
                        color: '#6b7280',
                      }}
                    >
                      <Clock style={{ width: '1rem', height: '1rem', marginRight: '0.5rem' }} />
                      <span>
                        {schedule.isAllDay
                          ? '하루 종일'
                          : `${formatDateTime(schedule.startAt || schedule.startDateTime)} - ${formatDateTime(
                              schedule.endAt || schedule.endDateTime
                            )}`}
                      </span>
                    </div>

                    {schedule.location && (
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          fontSize: '0.875rem',
                          color: '#6b7280',
                        }}
                      >
                        <MapPin style={{ width: '1rem', height: '1rem', marginRight: '0.5rem' }} />
                        <span>{schedule.location}</span>
                      </div>
                    )}

                    {schedule.memo && (
                      <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: 0 }}>
                        {schedule.memo}
                      </p>
                    )}
                  </div>
                </div>

                {/* 수정/삭제 버튼 */}
                <div style={{ display: 'flex', gap: '0.5rem', marginLeft: '1rem' }}>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleEdit(schedule);
                    }}
                    style={{
                      padding: '0.5rem',
                      color: '#2563eb',
                      backgroundColor: 'transparent',
                      border: 'none',
                      borderRadius: '0.5rem',
                      cursor: 'pointer',
                    }}
                  >
                    <Edit style={{ width: '1rem', height: '1rem' }} />
                  </button>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(schedule.id);
                    }}
                    style={{
                      padding: '0.5rem',
                      color: '#dc2626',
                      backgroundColor: 'transparent',
                      border: 'none',
                      borderRadius: '0.5rem',
                      cursor: 'pointer',
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
            <Calendar
              style={{
                width: '4rem',
                height: '4rem',
                color: '#d1d5db',
                margin: '0 auto 1rem auto',
              }}
            />
            <p style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>등록된 일정이 없습니다.</p>
            <p style={{ color: '#9ca3af' }}>새로운 일정을 추가해보세요!</p>
          </div>
        )}
      </div>

      {/* 상세 모달 */}
      {selectedSchedule && (
        <ScheduleDetailModal scheduleId={selectedSchedule} onClose={() => setSelectedSchedule(null)} />
      )}
    </div>
  );
};
