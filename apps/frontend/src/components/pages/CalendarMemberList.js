// src/components/calendar/CalendarMemberList.jsx
import React from 'react';
import { useCalendars } from '../../contexts/CalendarContext';
import { useAuth } from '../../contexts/AuthContext';

export const CalendarMemberList = ({ calendarId, onSelfLeave }) => {
  const { calendarMembers, removeCalendarMember, currentCalendar } = useCalendars();
  const { user } = useAuth();

  const isOwner = user.id === currentCalendar?.ownerId;

  const handleRemove = async (member, label) => {
    const ok = window.confirm(`${label}하시겠습니까?`);
    if (!ok) return;

    await removeCalendarMember(member.calendarMemberId);

    // 🔥 내가 탈퇴한 경우
    if (member.memberId === user.id) {
      onSelfLeave?.();
    }
  };

  return (
    <>
      {calendarMembers.map((m) => {
        const isMe = m.memberId === user.id;

        return (
          <div key={m.calendarMemberId}>
            <span>{m.memberName}{isMe && ' (나)'}</span>

            {isMe && (
              <button onClick={() => handleRemove(m, '탈퇴')}>
                탈퇴
              </button>
            )}

            {!isMe && isOwner && (
              <button onClick={() => handleRemove(m, '추방')}>
                추방
              </button>
            )}
          </div>
        );
      })}
    </>
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
