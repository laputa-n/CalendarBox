// src/pages/CalendarBoardPage.js
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useCalendars } from '../../contexts/CalendarContext';
import { useInvitedCalendars } from '../../hooks/useInvitedCalendars';

/* =========================
 *  Utils
 * ========================= */

const renderCalendarType = (type) => {
  switch (type) {
    case 'PERSONAL':
      return { label: '👤 개인', color: '#3b82f6' };
    case 'GROUP':
      return { label: '👥 그룹', color: '#10b981' };
    default:
      return { label: type, color: '#6b7280' };
  }
};

const renderVisibilityLabel = (visibility) => {
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

/* =========================
 *  Page
 * ========================= */

export const CalendarBoardPage = () => {
  const { calendars, loading, setDefaultCalendar } = useCalendars();
  const {
    invites,
    loading: inviteLoading,
    respondInvite,
  } = useInvitedCalendars();

  const navigate = useNavigate();

  if (loading) {
    return <p>캘린더를 불러오는 중...</p>;
  }

  // ✅ 기본 캘린더 설정
  const handleSetDefault = async (calendar) => {
    if (calendar.isDefault) return;

    const ok = window.confirm(
      `"${calendar.name}"을 기본 캘린더로 설정할까요?`
    );
    if (!ok) return;

    try {
      await setDefaultCalendar(calendar.id);
    } catch (e) {
      console.error('기본 캘린더 설정 실패', e);
    }
  };

  return (
    <div style={containerStyle}>
      <header style={headerStyle}>
        <h1 style={titleStyle}>📅 캘린더</h1>
        <p style={subtitleStyle}>
          내 캘린더 및 공유된 캘린더 목록입니다
        </p>
      </header>

      {/* =========================
          내 캘린더 목록
      ========================= */}
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
              onHistory={() => navigate(`/calendar/${calendar.id}/histories`)}  
              onSetDefault={handleSetDefault}
            />
          ))}
        </div>
      )}

      {/* =========================
          받은 캘린더 초대
      ========================= */}
      {!inviteLoading && invites.length > 0 && (
        <section style={{ marginTop: '3rem' }}>
          <h2 style={sectionTitleStyle}>📩 받은 캘린더 초대</h2>

          <div style={gridStyle}>
            {invites.map((invite) => (
              <div key={invite.calendarMemberId} style={cardStyle}>
                <div>
                  <h2 style={cardTitleStyle}>
                    {invite.calendarName}
                  </h2>

                  <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                    <span style={cardSubtitleStyle}>
                      {renderVisibilityLabel(invite.calendarVisibility)}
                    </span>
                    <span style={cardSubtitleStyle}>
                      초대자: {invite.inviterName}
                    </span>
                  </div>

                  <p style={inviteTimeStyle}>
                    {new Date(invite.createdAt).toLocaleString()}
                  </p>
                </div>

                <div style={cardButtonGroupStyle}>
                  <button
                    style={primaryButtonStyle}
                    onClick={() =>
                      respondInvite(invite.calendarMemberId, 'ACCEPT')
                    }
                  >
                    수락
                  </button>

                  <button
                    style={outlineButtonStyle}
                    onClick={() =>
                      respondInvite(invite.calendarMemberId, 'REJECT')
                    }
                  >
                    거절
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
};

/* =========================
 *  Components
 * ========================= */

const CalendarCard = ({ calendar, onOpen, onDetail, onHistory, onSetDefault }) => {
  const typeInfo = renderCalendarType(calendar.type);

  return (
    <div style={cardStyle}>
      <div>
        <h2 style={cardTitleStyle}>
          {calendar.name}
          {calendar.isDefault && (
            <span style={{ marginLeft: 6, color: '#10b981' }}>⭐ 기본</span>
          )}
        </h2>

        <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
          <span
            style={{
              fontSize: '0.75rem',
              padding: '2px 6px',
              borderRadius: 6,
              backgroundColor: '#f1f5f9',
              color: typeInfo.color,
              fontWeight: 500,
            }}
          >
            {typeInfo.label}
          </span>

          <span style={cardSubtitleStyle}>
            {renderVisibilityLabel(calendar.visibility)}
          </span>
        </div>
      </div>

      <div style={cardButtonGroupStyle}>
        <button onClick={onOpen} style={primaryButtonStyle}>
          캘린더 보기
        </button>

        <button onClick={onDetail} style={outlineButtonStyle}>
          상세
        </button>

        <button onClick={onHistory} style={outlineButtonStyle}>
          기록 보기
        </button>


        {!calendar.isDefault && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              onSetDefault(calendar);
            }}
            style={defaultButtonStyle}
          >
            ⭐ 기본으로 설정
          </button>
        )}
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

const containerStyle = { padding: '2rem' };
const headerStyle = { marginBottom: '2rem' };

const titleStyle = {
  fontSize: '1.75rem',
  fontWeight: '700',
  marginBottom: '0.25rem',
};

const subtitleStyle = { color: '#6b7280' };

const sectionTitleStyle = {
  fontSize: '1.25rem',
  fontWeight: 600,
  marginBottom: '1rem',
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

const inviteTimeStyle = {
  fontSize: '0.75rem',
  color: '#9ca3af',
  marginTop: 6,
};

const cardButtonGroupStyle = {
  display: 'flex',
  flexWrap: 'wrap',
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

const defaultButtonStyle = {
  padding: '0.5rem 0.75rem',
  backgroundColor: '#f9fafb',
  border: '1px solid #d1d5db',
  borderRadius: '0.5rem',
  cursor: 'pointer',
  fontSize: '0.75rem',
};

const emptyStateStyle = {
  textAlign: 'center',
  color: '#6b7280',
  padding: '3rem 0',
};
