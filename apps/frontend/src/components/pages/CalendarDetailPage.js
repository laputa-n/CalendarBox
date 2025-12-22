import React, { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useCalendars } from '../../contexts/CalendarContext';
import { useFriends } from '../../contexts/FriendContext';
import { useAuth } from '../../contexts/AuthContext';
import { CalendarMemberList } from './CalendarMemberList';


export const CalendarDetailPage = () => {
  const { calendarId } = useParams();
  const { user } = useAuth();

  const {
    calendars,
    currentCalendar,
    setCurrentCalendar,
    inviteCalendarMembers,
    fetchCalendarMembers,
    calendarMembers,
    loading,
  } = useCalendars();

  const { acceptedFriendships } = useFriends();

  const [inviteOpen, setInviteOpen] = useState(false);
  const [selectedIds, setSelectedIds] = useState([]);

  /* =========================
   *  URL → currentCalendar
   * ========================= */
  useEffect(() => {
    if (!calendarId || calendars.length === 0) return;

    const id = Number(calendarId);
    const target = calendars.find(c => c.id === id);

    if (target && currentCalendar?.id !== target.id) {
      setCurrentCalendar(target);
    }
  }, [calendarId, calendars, currentCalendar?.id, setCurrentCalendar]);

  /* =========================
   *  캘린더 멤버 조회
   * ========================= */
  useEffect(() => {
    if (!calendarId) return;
    fetchCalendarMembers(Number(calendarId), { page: 0, size: 50 });
  }, [calendarId, fetchCalendarMembers]);

  /* =========================
   *  친구 → 초대 대상 변환
   * ========================= */
  const inviteCandidates = useMemo(() => {
    if (!user) return [];

    return acceptedFriendships.map(f =>
      f.requesterId === user.id
        ? { memberId: f.receiverId, name: f.receiverName }
        : { memberId: f.requesterId, name: f.requesterName }
    );
  }, [acceptedFriendships, user]);

  const alreadyMemberIds = new Set(
    (calendarMembers || []).map(m => m.memberId)
  );

  const toggle = (id) => {
    setSelectedIds(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  };

const handleInvite = async () => {
  if (selectedIds.length === 0) {
    alert('초대할 친구를 선택해주세요.');
    return;
  }

  const res = await inviteCalendarMembers(Number(calendarId), selectedIds);
  const data = res?.data?.data ?? res?.data ?? res;

  alert(
    `초대 결과\n성공: ${data.successCount}명\n실패: ${data.failureCount}명`
  );

  // ✅ 여기 추가
  await fetchCalendarMembers(Number(calendarId), 0, 50);

  setSelectedIds([]);
  setInviteOpen(false);
};

  /* =========================
   *  Render
   * ========================= */
  return (
    <div style={{ padding: '2rem' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700 }}>
        {currentCalendar?.name}
      </h1>

      <button style={primaryBtn} onClick={() => setInviteOpen(true)}>
        멤버 초대
      </button>

       <CalendarMemberList calendarId={Number(calendarId)} />

      {/* 초대 모달 */}
      {inviteOpen && (
        <div style={overlay}>
          <div style={modal}>
            <h3 style={{ fontWeight: 700 }}>친구 초대</h3>

            <div style={listBox}>
              {inviteCandidates.map(f => {
                const disabled = alreadyMemberIds.has(f.memberId);

                return (
                  <label key={f.memberId} style={{ opacity: disabled ? 0.5 : 1 }}>
                    <input
                      type="checkbox"
                      disabled={disabled}
                      checked={selectedIds.includes(f.memberId)}
                      onChange={() => toggle(f.memberId)}
                    />
                    {f.name}
                    {disabled && ' (이미 멤버)'}
                  </label>
                );
              })}
            </div>

            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
              <button onClick={() => setInviteOpen(false)}>취소</button>
              <button style={primaryBtn} onClick={handleInvite}>초대</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

/* =========================
 *  Styles
 * ========================= */

const primaryBtn = {
  padding: '0.5rem 1rem',
  backgroundColor: '#2563eb',
  color: '#fff',
  border: 'none',
  borderRadius: 8,
};

const overlay = {
  position: 'fixed',
  inset: 0,
  background: 'rgba(0,0,0,0.4)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
};

const modal = {
  background: '#fff',
  padding: 16,
  borderRadius: 12,
  width: 420,
};

const listBox = {
  maxHeight: 300,
  overflow: 'auto',
  margin: '1rem 0',
  display: 'flex',
  flexDirection: 'column',
  gap: 6,
};
