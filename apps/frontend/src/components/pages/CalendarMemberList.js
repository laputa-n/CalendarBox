// src/components/calendar/CalendarMemberList.jsx
import React from 'react';
import { useCalendars } from '../../contexts/CalendarContext';
import { useAuth } from '../../contexts/AuthContext';

export const CalendarMemberList = ({ calendarId }) => {
  const { calendarMembers, removeCalendarMember, currentCalendar } = useCalendars();
  const { user } = useAuth();

  if (!calendarMembers || calendarMembers.length === 0) {
    return <p>참여 중인 멤버가 없습니다.</p>;
  }

  const isOwner = user.id === currentCalendar?.ownerId;

  const handleRemove = async (calendarMemberId, label) => {
    const ok = window.confirm(`${label}하시겠습니까?`);
    if (!ok) return;

    await removeCalendarMember(calendarMemberId);
  };

  return (
    <div style={{ marginTop: '1.5rem' }}>
      <h3 style={{ fontWeight: 600 }}>멤버 목록</h3>

      <ul style={{ marginTop: '0.75rem' }}>
        {calendarMembers.map((m) => {
          const isMe = m.memberId === user.id;

          return (
            <li
              key={m.calendarMemberId}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                padding: '0.5rem 0',
                borderBottom: '1px solid #e5e7eb',
              }}
            >
              <span>
                {m.memberName}
                {isMe && ' (나)'}
              </span>

              {/* 버튼 영역 */}
              {isMe && (
                <button
                  onClick={() => handleRemove(m.calendarMemberId, '탈퇴')}
                  style={dangerBtn}
                >
                  탈퇴
                </button>
              )}

              {!isMe && isOwner && (
                <button
                  onClick={() => handleRemove(m.calendarMemberId, '추방')}
                  style={dangerBtn}
                >
                  추방
                </button>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
};

const dangerBtn = {
  background: '#ef4444',
  color: '#fff',
  border: 'none',
  padding: '0.25rem 0.75rem',
  borderRadius: 6,
  cursor: 'pointer',
};
