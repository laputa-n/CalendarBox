import React, { useMemo, useState } from 'react';
import { Search, Plus, Trash2 } from 'lucide-react';

export default function ScheduleParticipantsSection({
  invitees,
  setInvitees,

  // 후보 리스트 (부모에서 받아오기)
  calendarMembers = [],
  friends = [],
  serviceUserResults = [],

  // 검색 콜백 (부모에서 API 연동)
  onSearchServiceUser,
  searchingServiceUser = false,
   myMemberId = null,
}) {
  const [activeTab, setActiveTab] = useState('CALENDAR'); // CALENDAR | FRIEND | SEARCH | NAME
  const [nameInput, setNameInput] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [nameOverrideMap, setNameOverrideMap] = useState({}); // memberId -> nameOverride

// ✅ memberId 추출을 최대한 유연하게
  const getMemberId = (m) =>
    m?.memberId ??
    m?.id ??
    m?.userId ??
    m?.targetMemberId ??
    m?.friendMemberId ??
    m?.member?.id ??
    null;

  // ✅ 화면 표시용 이름 우선순위
  const getDisplayName = (m) =>
    m?.name ??
    m?.friendName ?? 
    m?.nickname ??
    m?.memberName ??
    m?.member?.name ??
    m?.profileName ??
    m?.email ??
    m?.phoneNumber ??
    '이름없음';

  // ✅ 친구 ACCEPTED 판별(필드명이 다를 수 있어 여러 후보 대응)
const isAcceptedFriend = (f) => {
  const st =
    f?.status ??
    f?.friendStatus ??
    f?.relationStatus ??
    f?.requestStatus ??
    '';

  if (!st) return true; // ✅ 친구 API가 ACCEPTED만 주는 케이스 대응
  return String(st).toUpperCase() === 'ACCEPTED';
};
const filteredCalendarMembers = useMemo(() => {
  return (Array.isArray(calendarMembers) ? calendarMembers : [])
    .filter((m) => {
      const st = m?.status;
      return !st || String(st).toUpperCase() === 'ACCEPTED';
    });
}, [calendarMembers]);


  // ✅ (3) 내 아이디 제외 + (1) ACCEPTED만
  const filteredFriends = useMemo(() => {
    const mid = myMemberId == null ? null : String(myMemberId);
    return (Array.isArray(friends) ? friends : [])
      .filter(isAcceptedFriend)
      .filter((f) => {
        const fid = getMemberId(f);
        if (mid == null || fid == null) return true;
        return String(fid) !== mid;
      });
  }, [friends, myMemberId]);

 const inviteeKeySet = useMemo(() => {
    const s = new Set();
    (invitees || []).forEach((inv) => {
      if (inv?.type === 'SERVICE_USER') s.add(`U:${inv.memberId}`);
      if (inv?.type === 'NAME') s.add(`N:${inv.name}`);
    });
    return s;
  }, [invitees]);

  const addServiceUser = (member) => {
    const mid = getMemberId(member);
    if (!mid) return;

    // ✅ (3) 내 자신은 추가 금지 (UI에서 제외했더라도 안전장치)
    if (myMemberId != null && String(mid) === String(myMemberId)) return;

    const key = `U:${mid}`;
    if (inviteeKeySet.has(key)) return;

    setInvitees((prev) => [
      ...(prev || []),
      {
        type: 'SERVICE_USER',
        memberId: mid,
        displayName: getDisplayName(member), // ✅ (2) 이름 저장
        nameOverride: '',
      },
    ]);
  };

  const renderCandidateRow = (m) => {
    const mid = getMemberId(m);
    if (!mid) return null;

    // ✅ (3) 내 자신은 목록에서 제외(한 번 더)
    if (myMemberId != null && String(mid) === String(myMemberId)) return null;

    const disabled = inviteeKeySet.has(`U:${mid}`);
    const displayName = getDisplayName(m);
    return (
      <div
        key={mid}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 8,
          padding: '8px',
          borderRadius: 8,
          border: '1px solid #e5e7eb',
          background: '#f9fafb',
          marginBottom: 6,
          opacity: disabled ? 0.6 : 1,
        }}
      >
        <div style={{ minWidth: 0 }}>
          {/* ✅ (2) memberId 대신 이름 표시 */}
          <div style={{ fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {displayName}
          </div>

          {/* 보조정보는 email/phone만 (memberId 표시 제거) */}
          {(m.email || m.phoneNumber) && (
            <div style={{ fontSize: 12, color: '#6b7280' }}>
              {m.email || m.phoneNumber}
            </div>
          )}
        </div>

        <button
          type="button"
          onClick={() => addServiceUser(m)}
          disabled={disabled}
          style={{
            padding: '6px 10px',
            borderRadius: 8,
            border: 'none',
            cursor: disabled ? 'not-allowed' : 'pointer',
            background: '#e5e7eb',
          }}
          title="추가"
        >
          <Plus size={16} />
        </button>
      </div>
    );
  };

  const addNameInvitee = () => {
  const n = String(nameInput || '').trim();
  if (!n) return;

  const key = `N:${n}`;
  if (inviteeKeySet.has(key)) return;

  setInvitees((prev) => [...(prev || []), { type: 'NAME', name: n }]);
  setNameInput('');
};

  const removeInvitee = (inv) => {
    setInvitees((prev) =>
      (prev || []).filter((x) => {
      if (inv.type === 'SERVICE_USER')
  return !(x.type === 'SERVICE_USER' && String(x.memberId) === String(inv.memberId));

      })
    );
  };

  const setNameOverride = (memberId, value) => {
    setNameOverrideMap((prev) => ({ ...prev, [memberId]: value }));
    setInvitees((prev) =>
      (prev || []).map((x) => {
       if (x.type === 'SERVICE_USER' && String(x.memberId) === String(memberId)) {
  return { ...x, nameOverride: value };
}

        return x;
      })
    );
  };

  return (
    <div style={{ marginBottom: '1rem' }}>
      <label style={{ fontWeight: 500, fontSize: '0.875rem', marginBottom: '0.25rem', display: 'block' }}>
        👥 참가자 초대
      </label>

      {/* 선택된 초대 대상 */}
      {(invitees || []).length > 0 ? (
        <div style={{ marginBottom: 10 }}>
          {(invitees || []).map((inv) => (
            <div
              key={inv.type === 'SERVICE_USER' ? `U:${inv.memberId}` : `N:${inv.name}`}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 8,
                padding: '8px',
                borderRadius: 8,
                background: '#fff',
                border: '1px solid #e5e7eb',
                marginBottom: 6,
              }}
            >
              <div style={{ minWidth: 0, flex: 1 }}>
                {inv.type === 'SERVICE_USER' ? (
  <>
    <div style={{ fontWeight: 600 }}>
      {inv.displayName ? inv.displayName : `서비스 이용자 (memberId: ${inv.memberId})`}
    </div>
                    <div style={{ fontSize: 12, color: '#6b7280', marginTop: 4 }}>
                      별칭(선택):
                      <input
                        value={inv.nameOverride || ''}
                        onChange={(e) => setNameOverride(inv.memberId, e.target.value)}
                        placeholder="예: 아빠"
                        style={{
                          marginLeft: 8,
                          padding: '4px 8px',
                          borderRadius: 6,
                          border: '1px solid #d1d5db',
                          fontSize: 12,
                          width: 160,
                        }}
                      />
                    </div>
                  </>
                ) : (
                  <div style={{ fontWeight: 600 }}>이름으로 초대: {inv.name}</div>
                )}
              </div>

              <button
                type="button"
                onClick={() => removeInvitee(inv)}
                style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#ef4444' }}
                title="삭제"
              >
                <Trash2 size={16} />
              </button>
            </div>
          ))}
        </div>
      ) : (
        <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 10 }}>
          아직 초대 대상이 없습니다.
        </div>
      )}

      {/* 탭 */}
      <div style={{ display: 'flex', gap: 6, marginBottom: 10 }}>
        {[
          { k: 'CALENDAR', t: '캘린더 멤버' },
          { k: 'FRIEND', t: '친구' },
          { k: 'SEARCH', t: '서비스 유저 검색' },
          { k: 'NAME', t: '이름으로 추가' },
        ].map((x) => (
          <button
            key={x.k}
            type="button"
            onClick={() => setActiveTab(x.k)}
            style={{
              padding: '6px 10px',
              borderRadius: 999,
              border: '1px solid #e5e7eb',
              background: activeTab === x.k ? '#111827' : '#fff',
              color: activeTab === x.k ? '#fff' : '#111',
              cursor: 'pointer',
              fontSize: 12,
            }}
          >
            {x.t}
          </button>
        ))}
      </div>

      {/* 탭 컨텐츠 */}
      {activeTab === 'CALENDAR' && (
  <div>
    {filteredCalendarMembers.length > 0 ? (
      filteredCalendarMembers.map(renderCandidateRow)
    ) : (
      <div style={{ fontSize: 12, color: '#6b7280' }}>캘린더 멤버 목록이 없습니다.</div>
    )}
  </div>
)}


  {activeTab === 'FRIEND' && (
  <div>
    {Array.isArray(filteredFriends) && filteredFriends.length > 0 ? (
      filteredFriends.map(renderCandidateRow)
    ) : (
      <div style={{ fontSize: 12, color: '#6b7280' }}>친구 목록이 없습니다.</div>
    )}
  </div>
)}

      {activeTab === 'SEARCH' && (
        <div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
            <input
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="이메일/전화/이름으로 검색"
              style={{
                flex: 1,
                padding: '8px',
                borderRadius: 8,
                border: '1px solid #d1d5db',
                fontSize: 12,
              }}
            />
            <button
              type="button"
              onClick={() => onSearchServiceUser && onSearchServiceUser(searchInput)}
              disabled={!onSearchServiceUser || searchingServiceUser}
              style={{
                padding: '8px 10px',
                borderRadius: 8,
                border: 'none',
                cursor: onSearchServiceUser ? 'pointer' : 'not-allowed',
                background: '#e5e7eb',
              }}
              title="검색"
            >
              <Search size={16} />
            </button>
          </div>

          {Array.isArray(serviceUserResults) && serviceUserResults.length > 0 ? (
            serviceUserResults.map(renderCandidateRow)
          ) : (
            <div style={{ fontSize: 12, color: '#6b7280' }}>
              검색 결과가 없습니다.
            </div>
          )}
        </div>
      )}

      {activeTab === 'NAME' && (
        <div style={{ display: 'flex', gap: 8 }}>
          <input
            value={nameInput}
            onChange={(e) => setNameInput(e.target.value)}
            placeholder="이름으로 초대 : 아빠"
            style={{
              flex: 1,
              padding: '8px',
              borderRadius: 8,
              border: '1px solid #d1d5db',
              fontSize: 12,
            }}
          />
          <button
            type="button"
            onClick={addNameInvitee}
            style={{ padding: '8px 10px', borderRadius: 8, border: 'none', cursor: 'pointer', background: '#e5e7eb' }}
            title="추가"
          >
            <Plus size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
