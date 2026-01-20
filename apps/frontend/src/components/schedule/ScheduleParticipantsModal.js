// src/components/schedule/ScheduleParticipantsModal.js
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { X, Loader2, Search, Plus, Trash2 } from 'lucide-react';
import { ApiService } from '../../services/apiService';

const overlayStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  width: '100%',
  height: '100%',
  backgroundColor: 'rgba(0,0,0,0.5)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  zIndex: 1200,
};

const modalStyle = {
  backgroundColor: '#fff',
  padding: '1.25rem',
  borderRadius: 12,
  width: 520,
  maxHeight: '90vh',
  overflowY: 'auto',
};

const inputStyle = {
  width: '100%',
  padding: '0.5rem 0.75rem',
  borderRadius: 10,
  border: '1px solid #d1d5db',
  fontSize: 13,
};

const itemRow = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  background: '#f9fafb',
  borderRadius: 10,
  padding: '8px 10px',
  border: '1px solid #e5e7eb',
  marginBottom: 8,
  gap: 10,
};

const btn = {
  padding: '6px 10px',
  borderRadius: 10,
  border: '1px solid #e5e7eb',
  background: '#fff',
  cursor: 'pointer',
  fontSize: 12,
};

export default function ScheduleParticipantsModal({
  isOpen,
  onClose,
  scheduleId,
  hostMemberId = null, // host 강퇴 방지용(없으면 host 방지 로직은 약하게 동작)
  calendarId = null,   // ✅ 캘린더 멤버 조회용
}) {
  const unwrapData = useCallback((res) => {
    const body = res?.data ?? res; // axios vs fetch
    return body?.data ?? body;     // wrapper(data) vs plain
  }, []);

  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  // ====== ACCEPTED participants ======
  const [participants, setParticipants] = useState([]); // ACCEPTED만

  // ====== calendar members ======
  const [calendarMembersLoading, setCalendarMembersLoading] = useState(false);
  const [calendarMembers, setCalendarMembers] = useState([]);
  const [calendarMemberQuery, setCalendarMemberQuery] = useState('');

  // ====== service user search ======
  const [serviceUserQuery, setServiceUserQuery] = useState('');
  const [searching, setSearching] = useState(false);
  const [serviceUserResults, setServiceUserResults] = useState([]);

  // ====== name invite ======
  const [nameInvite, setNameInvite] = useState('');

  const acceptedSet = useMemo(() => {
    const s = new Set();
    (participants || []).forEach((p) => {
      const mid = p?.memberId ?? null;
      if (mid != null) s.add(String(mid));
    });
    return s;
  }, [participants]);

  // ✅ ACCEPTED 로드
  const loadAccepted = useCallback(async () => {
    if (!scheduleId) return;

    try {
      setLoading(true);

      const res = await ApiService.getScheduleParticipants(scheduleId, {
        status: 'ACCEPTED',
        page: 0,
        size: 100,
      });

      const data = unwrapData(res);
      const list = Array.isArray(data?.content)
        ? data.content
        : Array.isArray(data)
          ? data
          : [];

      setParticipants(list);
    } catch (e) {
      console.error('[getScheduleParticipants/ACCEPTED] failed', e);
      setParticipants([]);
    } finally {
      setLoading(false);
    }
  }, [scheduleId, unwrapData]);

  // ✅ 캘린더 멤버 로드
  const loadCalendarMembers = useCallback(async () => {
    if (!calendarId) {
      setCalendarMembers([]);
      return;
    }

    try {
      setCalendarMembersLoading(true);

      const res = await ApiService.getCalendarMembers(calendarId, 0, 200);
      const data = unwrapData(res);

      // 서버 형태가 {data:{content:[]}} / {content:[]} / [] 등 모두 대응
      const list = Array.isArray(data?.content)
        ? data.content
        : Array.isArray(data)
          ? data
          : [];

      setCalendarMembers(list);
    } catch (e) {
      console.error('[getCalendarMembers] failed', e);
      setCalendarMembers([]);
    } finally {
      setCalendarMembersLoading(false);
    }
  }, [calendarId, unwrapData]);

  // 모달 열릴 때 ACCEPTED + 캘린더 멤버 로드
  useEffect(() => {
    if (!isOpen) return;
    loadAccepted();
    loadCalendarMembers();
  }, [isOpen, loadAccepted, loadCalendarMembers]);

  // ✅ 캘린더 멤버 필터링
  const filteredCalendarMembers = useMemo(() => {
    const q = String(calendarMemberQuery || '').trim().toLowerCase();
    const list = Array.isArray(calendarMembers) ? calendarMembers : [];
    if (!q) return list;

    return list.filter((m) => {
      const name = String(m?.name ?? m?.nickname ?? '').toLowerCase();
      const email = String(m?.email ?? '').toLowerCase();
      const phone = String(m?.phoneNumber ?? '').toLowerCase();
      return name.includes(q) || email.includes(q) || phone.includes(q);
    });
  }, [calendarMembers, calendarMemberQuery]);

  // 서비스 유저 검색 debounce
  useEffect(() => {
    const q = String(serviceUserQuery || '').trim();
    if (!isOpen) return;

    if (!q) {
      setServiceUserResults([]);
      return;
    }

    const t = setTimeout(async () => {
      try {
        setSearching(true);

        const res = await ApiService.searchMembers(q, 0, 20);
        const list =
          res?.data?.data?.content ??
          res?.data?.content ??
          res?.data?.data ??
          [];

        const normalized = Array.isArray(list) ? list : [];
        setServiceUserResults(normalized);
      } catch (e) {
        console.error('[searchMembers] failed', e);
        setServiceUserResults([]);
      } finally {
        setSearching(false);
      }
    }, 300);

    return () => clearTimeout(t);
  }, [isOpen, serviceUserQuery, acceptedSet]);

  const addServiceUser = async (u) => {
    const memberId = u?.memberId ?? u?.id;
    if (!memberId) return alert('memberId를 찾을 수 없습니다.');

    if (acceptedSet.has(String(memberId))) return;

    try {
      setBusy(true);

      await ApiService.addScheduleParticipant(scheduleId, {
        mode: 'SERVICE_USER',
        memberId,
      });

      // UX: 초대 후 최신 반영
      await loadAccepted();
      alert('초대(추가) 완료');
    } catch (e) {
      console.error('[addScheduleParticipant/SERVICE_USER] failed', e);
      alert('초대(추가) 실패');
    } finally {
      setBusy(false);
    }
  };

  const addByName = async () => {
    const name = String(nameInvite || '').trim();
    if (!name) return;

    try {
      setBusy(true);

      await ApiService.addScheduleParticipant(scheduleId, {
        mode: 'NAME',
        name,
      });

      setNameInvite('');
      await loadAccepted();
      alert('이름 초대 완료');
    } catch (e) {
      console.error('[addScheduleParticipant/NAME] failed', e);
      alert('이름 초대 실패');
    } finally {
      setBusy(false);
    }
  };

  const kick = async (p) => {
    const participantId = p?.scheduleParticipantId ?? p?.id;
    if (!participantId) return;

    // host 삭제 방지
    const mid = p?.memberId ?? null;
    if (hostMemberId && mid != null && String(mid) === String(hostMemberId)) {
      alert('호스트는 삭제할 수 없습니다.');
      return;
    }

    const ok = window.confirm(`${p?.name ?? '참여자'} 님을 강퇴할까요?`);
    if (!ok) return;

    try {
      setBusy(true);
      await ApiService.removeScheduleParticipant(scheduleId, participantId);
      await loadAccepted();
      alert('강퇴 완료');
    } catch (e) {
      console.error('[removeScheduleParticipant] failed', e);
      alert('강퇴 실패');
    } finally {
      setBusy(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div style={overlayStyle}>
      <div style={modalStyle}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 800 }}>참여자 (ACCEPTED)</h3>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', cursor: 'pointer' }} title="닫기" type="button">
            <X />
          </button>
        </div>

        {/* ACCEPTED 목록 */}
        <div style={{ marginBottom: 14 }}>
          {loading ? (
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', color: '#6b7280' }}>
              <Loader2 style={{ width: 16, height: 16, animation: 'spin 1s linear infinite' }} />
              불러오는 중...
            </div>
          ) : participants.length === 0 ? (
            <div style={{ fontSize: 13, color: '#6b7280' }}>ACCEPTED 참여자가 없습니다.</div>
          ) : (
            participants.map((p) => {
              const isHost = hostMemberId && String(p?.memberId ?? '') === String(hostMemberId);
              return (
                <div key={p.scheduleParticipantId ?? p.id} style={itemRow}>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {p.name ?? `memberId:${p.memberId ?? '-'}`}{isHost ? ' (HOST)' : ''}
                    </div>
                    <div style={{ fontSize: 12, color: '#6b7280' }}>memberId: {p.memberId ?? '-'}</div>
                  </div>

                  <button
                    type="button"
                    onClick={() => kick(p)}
                    disabled={busy || isHost}
                    style={{ ...btn, borderColor: '#fecaca', background: '#fee2e2', color: '#b91c1c' }}
                    title="강퇴"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              );
            })
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button type="button" onClick={loadAccepted} style={btn} disabled={loading}>
              새로고침
            </button>
          </div>
        </div>

        <hr style={{ margin: '14px 0' }} />

        {/* ✅ 캘린더 멤버 초대 */}
        <div style={{ marginBottom: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <div style={{ fontSize: 13, fontWeight: 700 }}>캘린더 멤버 초대</div>
            <button type="button" onClick={loadCalendarMembers} style={btn} disabled={calendarMembersLoading || busy || !calendarId}>
              새로고침
            </button>
          </div>

          {!calendarId ? (
            <div style={{ fontSize: 12, color: '#6b7280' }}>
           
            </div>
          ) : (
            <>
              <input
                value={calendarMemberQuery}
                onChange={(e) => setCalendarMemberQuery(e.target.value)}
                placeholder="캘린더 멤버 필터(이름/이메일/전화)"
                style={{ ...inputStyle, marginBottom: 8 }}
                disabled={calendarMembersLoading || busy}
              />

              {calendarMembersLoading ? (
                <div style={{ display: 'flex', gap: 8, alignItems: 'center', color: '#6b7280' }}>
                  <Loader2 style={{ width: 16, height: 16, animation: 'spin 1s linear infinite' }} />
                  캘린더 멤버 불러오는 중...
                </div>
              ) : filteredCalendarMembers.length === 0 ? (
                <div style={{ fontSize: 12, color: '#6b7280' }}>표시할 캘린더 멤버가 없습니다.</div>
              ) : (
                filteredCalendarMembers.map((m, idx) => {
                  const memberId = m?.memberId ?? m?.id;
                  const already = memberId != null && acceptedSet.has(String(memberId));

                  return (
                    <div key={memberId ?? idx} style={itemRow}>
                      <div style={{ minWidth: 0 }}>
                        <div style={{ fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {m?.name ?? m?.nickname ?? m?.email ?? m?.phoneNumber ?? `memberId:${memberId ?? '-'}`}
                        </div>
                        <div style={{ fontSize: 12, color: '#6b7280' }}>
                          id: {memberId ?? '-'} {m?.email ? ` · ${m.email}` : ''} {m?.phoneNumber ? ` · ${m.phoneNumber}` : ''}
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={() => addServiceUser({ memberId })}
                        disabled={busy || already || memberId == null}
                        style={{ ...btn, background: already ? '#f3f4f6' : '#dbeafe' }}
                        title={already ? '이미 참여 중' : '추가'}
                      >
                        <Plus size={16} />
                      </button>
                    </div>
                  );
                })
              )}
            </>
          )}
        </div>

        <hr style={{ margin: '14px 0' }} />

        {/* 서비스 유저 검색 */}
        <div style={{ marginBottom: 14 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>서비스 유저 검색 초대</div>

          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <Search style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', width: 16, height: 16, color: '#6b7280' }} />
              <input
                value={serviceUserQuery}
                onChange={(e) => setServiceUserQuery(e.target.value)}
                placeholder="이메일/전화/이름"
                style={{ ...inputStyle, paddingLeft: 34 }}
                disabled={busy}
              />
            </div>
          </div>

          {searching ? (
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', color: '#6b7280' }}>
              <Loader2 style={{ width: 16, height: 16, animation: 'spin 1s linear infinite' }} />
              검색 중...
            </div>
          ) : serviceUserResults.length > 0 ? (
            serviceUserResults.map((u, idx) => {
              const memberId = u?.memberId ?? u?.id;
              const disabled = memberId != null && acceptedSet.has(String(memberId));

              return (
                <div key={memberId ?? idx} style={itemRow}>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {u.name ?? u.nickname ?? u.email ?? u.phoneNumber ?? '사용자'}
                    </div>
                    <div style={{ fontSize: 12, color: '#6b7280' }}>id: {memberId ?? '-'}</div>
                  </div>

                  <button
                    type="button"
                    onClick={() => addServiceUser(u)}
                    disabled={busy || disabled}
                    style={{ ...btn, background: disabled ? '#f3f4f6' : '#dbeafe' }}
                    title={disabled ? '이미 참여 중' : '추가'}
                  >
                    <Plus size={16} />
                  </button>
                </div>
              );
            })
          ) : null}
        </div>

        {/* 이름 초대 */}
        <div>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>이름으로 초대</div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              value={nameInvite}
              onChange={(e) => setNameInvite(e.target.value)}
              placeholder="예: 엄마"
              style={inputStyle}
              disabled={busy}
            />
            <button type="button" onClick={addByName} disabled={busy} style={{ ...btn, background: '#dbeafe' }}>
              추가
            </button>
          </div>
          <div style={{ fontSize: 12, color: '#6b7280', marginTop: 6 }}>
            이름 초대는 상대가 수락하기 전까지 ACCEPTED 목록에 보이지 않을 수 있습니다.
          </div>
        </div>
      </div>
    </div>
  );
}
