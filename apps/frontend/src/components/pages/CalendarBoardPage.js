// src/pages/CalendarBoardPage.js
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useCalendars } from '../../contexts/CalendarContext';

export const CalendarBoardPage = () => {
  const { calendars, loading } = useCalendars();
  const navigate = useNavigate();

  if (loading) {
    return <p>캘린더를 불러오는 중...</p>;
  }

  return (
    <div style={containerStyle}>
      <header style={headerStyle}>
        <h1 style={titleStyle}>📅 캘린더</h1>
        <p style={subtitleStyle}>
          내 캘린더 및 공유된 캘린더 목록입니다
        </p>
      </header>

      {calendars.length === 0 ? (
        <EmptyState />
      ) : (
        <div style={gridStyle}>
          {calendars.map((calendar) => (
            <CalendarCard
              key={calendar.id}
              calendar={calendar}
              onOpen={() => navigate(`/calendar/${calendar.id}`)}
              onDetail={() => navigate(`/calendar/${calendar.id}/detail`)}
            />
          ))}
        </div>
      )}
    </div>
  );
};

/* =========================
 *  Components
 * ========================= */

const CalendarCard = ({ calendar, onOpen , onDetail }) => {
  const isPublic = calendar.visibility === 'PUBLIC';

  return (
    <div style={cardStyle}>
      <div>
        <h2 style={cardTitleStyle}>{calendar.name}</h2>
        <p style={cardSubtitleStyle}>
          {isPublic ? '👥 공유 캘린더' : '🔒 개인 캘린더'}
        </p>
      </div>

      <div style={cardButtonGroupStyle}>
        <button onClick={onOpen} style={primaryButtonStyle}>
          캘린더 보기
        </button>

        {/* TODO: 캘린더 상세 모달 */}
       <button
  onClick={onDetail}
  style={outlineButtonStyle}
>
  상세
</button>
      </div>
    </div>
  );
};

const EmptyState = () => (
  <div style={emptyStateStyle}>
    <p>아직 생성된 캘린더가 없습니다.</p>
  </div>
);

/* =========================
 *  Styles
 * ========================= */

const containerStyle = {
  padding: '2rem',
};

const headerStyle = {
  marginBottom: '2rem',
};

const titleStyle = {
  fontSize: '1.75rem',
  fontWeight: '700',
  marginBottom: '0.25rem',
};

const subtitleStyle = {
  color: '#6b7280',
};

const gridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
  gap: '1rem',
};

const cardStyle = {
  backgroundColor: '#fff',
  padding: '1.5rem',
  borderRadius: '0.75rem',
  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'space-between',
};

const cardTitleStyle = {
  fontSize: '1.125rem',
  fontWeight: '600',
};

const cardSubtitleStyle = {
  fontSize: '0.875rem',
  color: '#6b7280',
  marginTop: '0.25rem',
};

const cardButtonGroupStyle = {
  display: 'flex',
  gap: '0.5rem',
  marginTop: '1.25rem',
};

const primaryButtonStyle = {
  padding: '0.5rem 1rem',
  backgroundColor: '#2563eb',
  color: '#fff',
  borderRadius: '0.5rem',
  border: 'none',
  cursor: 'pointer',
  fontSize: '0.875rem',
};

const outlineButtonStyle = {
  padding: '0.5rem 1rem',
  backgroundColor: '#fff',
  border: '1px solid #d1d5db',
  borderRadius: '0.5rem',
  cursor: 'pointer',
  fontSize: '0.875rem',
};

const emptyStateStyle = {
  textAlign: 'center',
  color: '#6b7280',
  padding: '3rem 0',
};
