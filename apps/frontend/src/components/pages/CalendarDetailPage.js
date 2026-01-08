// src/pages/calendar/CalendarDetailPage.jsx
import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ApiService } from '../../services/apiService';

export const CalendarDetailPage = () => {
  const { calendarId } = useParams();
  const navigate = useNavigate();

  const [calendar, setCalendar] = useState(null);
  const [loading, setLoading] = useState(true);

  /* =========================
   *  캘린더 상세 조회
   * ========================= */
  useEffect(() => {
    const fetchDetail = async () => {
      try {
        const res = await ApiService.getCalendarById(calendarId);
        const data = res?.data?.data ?? res?.data ?? res;
        setCalendar(data);
      } catch (e) {
        console.error('캘린더 상세 조회 실패:', e);
      } finally {
        setLoading(false);
      }
    };

    fetchDetail();
  }, [calendarId]);

  if (loading) {
    return <p style={{ padding: '2rem' }}>불러오는 중...</p>;
  }

  if (!calendar) {
    return <p style={{ padding: '2rem' }}>캘린더 정보를 찾을 수 없습니다.</p>;
  }

  /* =========================
   *  Render
   * ========================= */
  return (
    <div style={{ padding: '2rem', maxWidth: 640 }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700 }}>
        {calendar.name}
      </h1>

      <div style={{ marginTop: '1.5rem', lineHeight: 1.9 }}>
        <InfoRow label="캘린더 ID" value={calendar.calendarId} />
        <InfoRow label="타입" value={calendar.type} />
        <InfoRow
          label="공개 범위"
          value={renderVisibility(calendar.visibility)}
        />
        <InfoRow
          label="멤버 수"
          value={`${calendar.memberCount} 명`}
        />
        <InfoRow
          label="생성일"
          value={formatDate(calendar.createdAt)}
        />
        <InfoRow
          label="수정일"
          value={formatDate(calendar.updatedAt)}
        />
      </div>

      <div style={{ marginTop: '2rem', display: 'flex', gap: 8 }}>
        <button onClick={() => navigate(-1)}>← 뒤로가기</button>
        <button onClick={() => navigate(`/calendar/${calendarId}/members`)}>
          멤버 관리
        </button>
      </div>
    </div>
  );
};

/* =========================
 *  Sub Components
 * ========================= */

const InfoRow = ({ label, value }) => (
  <div style={{ display: 'flex', gap: 12 }}>
    <div style={{ width: 100, color: '#6b7280' }}>{label}</div>
    <div>{value}</div>
  </div>
);

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

const formatDate = (iso) => {
  if (!iso) return '-';
  return new Date(iso).toLocaleString();
};
