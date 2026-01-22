// src/components/schedule/ScheduleParticipantsModal.js
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { X, Loader2, Search, Plus } from 'lucide-react';
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
}) {
  const unwrapData = useCallback((res) => {
    const body = res?.data ?? res; // axios vs fetch
    return body?.data ?? body; // wrapper(data) vs plain
  }, []);

  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  // ✅ 이제 ACCEPTED만이 아니라, ACCEPTED/INVITED 모두 보여주기
  const [participants, setParticipants] = useState([]);
  const [serviceUserQuery, setServiceUserQuery] = useState('');
  const [searching, setSearching] = useState(false);
  const [serviceUserResults, setServiceUserResults] = useState([]);
  const [nameInvite, setNameInvite] = useState('');

  const getParticipantId = (p) => p?.scheduleParticipantId ?? p?.id ?? null;

  const deriveStatus = (p) => {
    // status가 없으면 NAME 초대(not user)는 수락됨처럼 보이게 처리(UI 깨짐 방지)
    const st = p?.status;
    if (st) return String(st).toUpperCase();
    if (p?.memberId == null) return 'ACCEPTED';
    return '';
  };

  const statusLabel = (st) => {
    const s = String(st || '').toUpperCase();
    if (s === 'ACCEPTED') return '수락됨';
    if (s === 'INVITED') return '초대됨';
    if (s === 'REJECTED') return '거절됨';
    return s || '-';
  };

  const displayName = (p) => {
    const name = p?.name ?? p?.memberName ?? p?.inviteeName ?? p?.friendName ?? '';
    const isNotUser = p?.memberId == null; // ✅ not user
    return `${name || '이름없음'}${isNotUser ? '(not user)' : ''}`;
  };

  // ✅ 이미 참여중(수락/초대 포함)인 memberId Set -> 중복 초대 방지
  const existingMemberIdSet = useMemo(() => {
    const s = new Set();
    (participants || []).forEach((p) => {
      const mid = p?.memberId;
      if (mid != null) s.add(String(mid));
    });
    return s;
  }, [participants]);

  // ✅ ACCEPTED + INVITED 둘 다 불러오기 (API가 status 필수인 경우를 대비해 2번 호출)
  const loadParticipants = useCallback(async () => {
    if (!scheduleId) return;

    try {
      setLoading(true);

      const [aRes, iRes] = await Promise.allSettled([
        ApiService.getScheduleParticipants(scheduleId, { status: 'ACCEPTED', page: 0, size: 200 }),
        ApiService.getScheduleParticipants(scheduleId, { status: 'INVITED', page: 0, size: 200 }),
      ]);

      const toList = (r) => {
        if (!r || r.status !== 'fulfilled') return [];
        const data = unwrapData(r.value);
        if (Array.isArray(data?.content)) return data.content;
        if (Array.isArray(data)) return data;
        return [];
      };

      const list = [...toList(aRes), ...toList(iRes)];

      // ✅ 중복 제거 + 정렬(수락됨 먼저)
      const map = new Map();
      list.forEach((p) => {
        const pid = getParticipantId(p);
        if (pid != null) map.set(String(pid), p);
      });

      const merged = Array.from(map.values()).sort((x, y) => {
        const rx = deriveStatus(x) === 'ACCEPTED' ? 0 : 1;
        const ry = deriveStatus(y) === 'ACCEPTED' ? 0 : 1;
        return rx - ry;
      });

      setParticipants(merged);
    } catch (e) {
      console.error('[getScheduleParticipants] failed', e);
      setParticipants([]);
    } finally {
      setLoading(false);
    }
  }, [scheduleId, unwrapData]);

  useEffect(() => {
    if (!isOpen) return;
    loadParticipants();
  }, [isOpen, loadParticipants]);

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
  }, [isOpen, serviceUserQuery]);

  const addServiceUser = async (u) => {
    const memberId = u?.memberId ?? u?.id;
    if (!memberId) return alert('memberId를 찾을 수 없습니다.');

    if (existingMemberIdSet.has(String(memberId))) return;

    try {
      setBusy(true);

      await ApiService.addScheduleParticipant(scheduleId, {
        mode: 'SERVICE_USER',
        memberId,
      });

      setServiceUserQuery('');
      setServiceUserResults([]);
      await loadParticipants();
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
      await loadParticipants();
      alert('이름 초대 완료');
    } catch (e) {
      console.error('[addScheduleParticipant/NAME] failed', e);
      alert('이름 초대 실패');
    } finally {
      setBusy(false);
    }
  };

  const removeOrCancel = async (p) => {
    const participantId = getParticipantId(p);
    if (!participantId) return;

    const st = deriveStatus(p); // ACCEPTED / INVITED / ...
    const mid = p?.memberId ?? null;

    // host 삭제 방지
    if (hostMemberId && mid != null && String(mid) === String(hostMemberId)) {
      alert('호스트는 삭제할 수 없습니다.');
      return;
    }

    const who = displayName(p);
    const msg =
      st === 'ACCEPTED'
        ? `${who} 님을 강퇴하시겠습니까?`
        : `${who} 님의 초대를 취소하시겠습니까?`;

    const ok = window.confirm(msg);
    if (!ok) return;

    try {
      setBusy(true);
      // ✅ 정식: removeScheduleParticipant (수락자=강퇴 / 초대자=초대취소 동일 API로 처리)
      await ApiService.removeScheduleParticipant(scheduleId, participantId);
      await loadParticipants();
      alert(st === 'ACCEPTED' ? '강퇴 완료' : '초대 취소 완료');
    } catch (e) {
      console.error('[removeScheduleParticipant] failed', e);
      alert(st === 'ACCEPTED' ? '강퇴 실패' : '초대 취소 실패');
    } finally {
      setBusy(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div style={overlayStyle}>
      <div style={modalStyle}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 12,
          }}
        >
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 800 }}>
            참여자
          </h3>
          <button
            onClick={onClose}
            style={{ background: 'transparent', border: 'none', cursor: 'pointer' }}
            title="닫기"
            type="button"
          >
            <X />
          </button>
        </div>

        {/* 목록 */}
        <div style={{ marginBottom: 14 }}>
          {loading ? (
            <div style={{ display: 'flex', gap: 8, alignItems: 'center', color: '#6b7280' }}>
              <Loader2 style={{ width: 16, height: 16, animation: 'spin 1s linear infinite' }} />
              불러오는 중...
            </div>
          ) : participants.length === 0 ? (
            <div style={{ fontSize: 13, color: '#6b7280' }}>참여자가 없습니다.</div>
          ) : (
            participants.map((p) => {
              const pid = getParticipantId(p);
              const st = deriveStatus(p);
              const isHost =
                hostMemberId && String(p?.memberId ?? '') === String(hostMemberId);

              return (
                <div key={pid ?? `${p?.name}-${Math.random()}`} style={itemRow}>
                  <div style={{ minWidth: 0 }}>
                    <div
                      style={{
                        fontWeight: 800,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {displayName(p)} {isHost ? '(HOST)' : ''}
                    </div>

                    {/* ✅ memberId 대신 상태 표시 */}
                    <div style={{ fontSize: 12, color: '#6b7280' }}>
                      상태: {statusLabel(st)}
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => removeOrCancel(p)}
                    disabled={busy || isHost}
                    style={{
                      ...btn,
                      borderColor: '#fecaca',
                      background: '#fee2e2',
                      color: '#b91c1c',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      width: 36,
                      height: 32,
                      padding: 0,
                    }}
                    title={st === 'ACCEPTED' ? '강퇴' : '초대 취소'}
                  >
                    {/* ✅ 휴지통 -> X */}
                    <X size={16} />
                  </button>
                </div>
              );
            })
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button type="button" onClick={loadParticipants} style={btn} disabled={loading}>
              새로고침
            </button>
          </div>
        </div>

        <hr style={{ margin: '14px 0' }} />

        {/* 서비스 유저 검색 */}
        <div style={{ marginBottom: 14 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>
            서비스 유저 검색 초대
          </div>

          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 8 }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <Search
                style={{
                  position: 'absolute',
                  left: 10,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  width: 16,
                  height: 16,
                  color: '#6b7280',
                }}
              />
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
              const disabled = memberId != null && existingMemberIdSet.has(String(memberId));

              return (
                <div key={memberId ?? idx} style={itemRow}>
                  <div style={{ minWidth: 0 }}>
                    <div
                      style={{
                        fontWeight: 700,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {u.name ?? u.nickname ?? u.email ?? u.phoneNumber ?? '사용자'}
                    </div>
                    <div style={{ fontSize: 12, color: '#6b7280' }}>
                      {/* 여기서는 id 표시해도 되지만 원하시면 제거 가능 */}
                      id: {memberId ?? '-'}
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => addServiceUser(u)}
                    disabled={busy || disabled}
                    style={{ ...btn, background: disabled ? '#f3f4f6' : '#dbeafe' }}
                    title={disabled ? '이미 참여/초대됨' : '추가'}
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
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 8 }}>
            이름으로 추가
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input
              value={nameInvite}
              onChange={(e) => setNameInvite(e.target.value)}
              placeholder="예: 아빠"
              style={inputStyle}
              disabled={busy}
            />
            <button
              type="button"
              onClick={addByName}
              disabled={busy}
              style={{ ...btn, background: '#dbeafe' }}
            >
              추가
            </button>
          </div>
          
        </div>
      </div>
    </div>
  );
}
