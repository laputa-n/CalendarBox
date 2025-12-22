import React, { useEffect, useState } from 'react';
import { X, Users, Loader2 } from 'lucide-react';
import { formatDateTime } from '../../utils/dateUtils';
import { ApiService } from '../../services/apiService';

export const ScheduleDetailModal = ({ schedule, onClose }) => {
  /** =========================
   * 상태
   * ========================= */
  const [participants, setParticipants] = useState([]);
  const [participantsLoading, setParticipantsLoading] = useState(false);

  const [inviteName, setInviteName] = useState('');
  const [inviting, setInviting] = useState(false);

  const [respondingId, setRespondingId] = useState(null);

  /** =========================
   * 참여자 목록 조회
   * ========================= */
  const fetchParticipants = async () => {
    if (!schedule?.id) return;

    try {
      setParticipantsLoading(true);
      const res = await ApiService.getScheduleParticipants(schedule.id);
      setParticipants(res.data?.content || []);
    } catch (e) {
      console.error('참여자 목록 조회 실패', e);
      setParticipants([]);
    } finally {
      setParticipantsLoading(false);
    }
  };

  /** =========================
   * 일정 변경 시 참여자 재조회
   * ========================= */
  useEffect(() => {
    fetchParticipants();
  }, [schedule]);

  /** =========================
   * 이름으로 멤버 초대
   * ========================= */
  const handleInviteByName = async () => {
    if (!inviteName.trim()) {
      alert('이름을 입력해주세요.');
      return;
    }

    try {
      setInviting(true);

      await ApiService.addScheduleParticipant(schedule.id, {
        mode: 'NAME',
        name: inviteName.trim(),
      });

      setInviteName('');
      await fetchParticipants();
    } catch (e) {
      console.error('멤버 초대 실패', e);
      alert(e.message || '멤버 초대 실패');
    } finally {
      setInviting(false);
    }
  };

  /** =========================
   * 초대 수락 / 거절
   * ========================= */
  const handleRespond = async (participantId, action) => {
    try {
      setRespondingId(participantId);

      await ApiService.respondToScheduleInvite(
        schedule.id,
        participantId,
        action // 'ACCEPT' | 'REJECT'
      );

      await fetchParticipants();
    } catch (e) {
      console.error('응답 처리 실패', e);
      alert('응답 처리 실패');
    } finally {
      setRespondingId(null);
    }
  };

  if (!schedule) return null;

  /** =========================
   * 스타일
   * ========================= */
  const overlayStyle = {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.45)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 9999,
    padding: '1rem',
  };

  const modalStyle = {
    width: '100%',
    maxWidth: 720,
    background: '#fff',
    borderRadius: 14,
    boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
    overflow: 'hidden',
  };

  const headerStyle = {
    padding: '1rem 1.25rem',
    borderBottom: '1px solid #e5e7eb',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  };

  const bodyStyle = {
    padding: '1.25rem',
  };

  const sectionTitleStyle = {
    fontSize: '0.95rem',
    fontWeight: 700,
    marginBottom: '0.5rem',
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
  };

  /** =========================
   * 렌더
   * ========================= */
  return (
    <div style={overlayStyle} onClick={onClose}>
      <div style={modalStyle} onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div style={headerStyle}>
          <div>
            <strong>{schedule.title}</strong>
            <div style={{ fontSize: '0.85rem', color: '#6b7280' }}>
              {schedule.isAllDay
                ? '하루 종일'
                : `${formatDateTime(schedule.startAt)} ~ ${formatDateTime(
                    schedule.endAt
                  )}`}
            </div>
          </div>
          <button onClick={onClose} style={{ border: 'none', background: 'none' }}>
            <X />
          </button>
        </div>

        {/* Body */}
        <div style={bodyStyle}>
          {/* 참여자 */}
          <div>
            <div style={sectionTitleStyle}>
              <Users size={18} />
              참여자
            </div>

            {/* 초대 */}
            <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
              <input
                type="text"
                placeholder="이름으로 초대"
                value={inviteName}
                onChange={(e) => setInviteName(e.target.value)}
                style={{
                  flex: 1,
                  padding: '0.5rem',
                  border: '1px solid #d1d5db',
                  borderRadius: 6,
                }}
              />
              <button
                onClick={handleInviteByName}
                disabled={inviting}
                style={{
                  padding: '0.5rem 0.75rem',
                  background: '#2563eb',
                  color: '#fff',
                  border: 'none',
                  borderRadius: 6,
                }}
              >
                {inviting ? '초대 중...' : '초대'}
              </button>
            </div>

            {/* 목록 */}
            {participantsLoading ? (
              <Loader2 style={{ animation: 'spin 1s linear infinite' }} />
            ) : participants.length === 0 ? (
              <div style={{ color: '#6b7280' }}>참여자가 없습니다.</div>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0 }}>
                {participants.map((p) => (
                  <li
                    key={p.scheduleParticipantId}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '0.4rem 0',
                      borderBottom: '1px solid #e5e7eb',
                    }}
                  >
                    <span>{p.name}</span>

                    {p.status === 'INVITED' ? (
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button
                          onClick={() =>
                            handleRespond(p.scheduleParticipantId, 'ACCEPT')
                          }
                          disabled={respondingId === p.scheduleParticipantId}
                        >
                          수락
                        </button>
                        <button
                          onClick={() =>
                            handleRespond(p.scheduleParticipantId, 'REJECT')
                          }
                          disabled={respondingId === p.scheduleParticipantId}
                        >
                          거절
                        </button>
                      </div>
                    ) : (
                      <span style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                        {p.status}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
