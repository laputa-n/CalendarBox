// src/pages/calendar/CalendarMemberList.jsx


import React, { useEffect, useMemo, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ApiService } from "../../services/apiService";



export const CalendarMemberList = () => {
  
  
  const { calendarId } = useParams();
  const navigate = useNavigate();
  const [members, setMembers] = useState([]);
  const [membersLoading, setMembersLoading] = useState(true);

  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [myId, setMyId] = useState(null);
  const [ownerId, setOwnerId] = useState(null);
  const [selectedMemberIds, setSelectedMemberIds] = useState([]);
  const [inviteLoading, setInviteLoading] = useState(false);
  const [friends, setFriends] = useState([]);
  const [friendsLoading, setFriendsLoading] = useState(true);
  console.log("📌 CalendarMemberList 렌더됨");
console.log("calendarId:", calendarId);
console.log("members:", members);
const fetchMembers = async () => {
  try {
    setMembersLoading(true);

    const res = await ApiService.getCalendarMembers(calendarId, {
      status: "ACCEPTED",
    });

    const content = res?.data?.content ?? [];
    setMembers(content);

    // content 안에 중복 포함된 메타 정보
    if (content.length > 0) {
      setMyId(Number(content[0].myId));
      setOwnerId(Number(content[0].ownerId));
    }
  } catch (e) {
    console.error("캘린더 멤버 조회 실패", e);
  } finally {
    setMembersLoading(false);
  }
};

  useEffect(() => {
    fetchMembers();
  }, [calendarId]);

  useEffect(() => {
  console.log("🔁 members 변경됨:", members);
}, [members]);

const fetchFriends = async () => {
  try {
    setFriendsLoading(true);

    const res = await ApiService.getFriends(1, 50);

    // 🔥 여기 중요
    const content = res?.data?.content ?? [];

    console.log("👥 친구 목록:", content);
    setFriends(content);
  } catch (e) {
    console.error("친구 목록 조회 실패", e);
  } finally {
    setFriendsLoading(false);
  }
};


useEffect(() => {
  fetchFriends();
}, []);
  const memberIdSet = useMemo(() => {
  return new Set(members.map((m) => m.memberId));
   console.log("🔁 members 변경됨:", members);
}, [members]);



  const handleSearch = async (query) => {
    setSearchQuery(query);

    if (!query.trim()) {
      setSearchResults([]);
      return;
    }

    try {
      setSearchLoading(true);
      console.log("검색요청:", query);
      const res = await ApiService.searchMembers(query);
      console.log("검색응답 raw:", res?.data);
      setSearchResults(res?.data?.content || []);
    } catch (e) {
      console.error("회원 검색 실패", e);
    } finally {
      setSearchLoading(false);
    }
  };


  const toggleSelect = (memberId) => {
    setSelectedMemberIds((prev) =>
      prev.includes(memberId)
        ? prev.filter((id) => id !== memberId)
        : [...prev, memberId]
    );
  };

  const clearSelection = () => setSelectedMemberIds([]);
 const handleInvite = async () => {
    if (selectedMemberIds.length === 0) {
      alert("초대할 멤버를 선택하세요.");
      return;
    }

    try {
      await ApiService.inviteCalendarMembers(
        calendarId,
        selectedMemberIds
      );
      alert("초대 완료");
      setSelectedMemberIds([]);
      fetchMembers();
    } catch (e) {
      console.error("초대 실패", e);
      alert("초대 실패");
    }
  };

  const handleRespond = async (calendarMemberId, action) => {
    try {
      await ApiService.respondCalendarInvite(
        calendarMemberId,
        action
      );
      fetchMembers();
    } catch (e) {
      console.error("초대 응답 실패", e);
    }
  };

  /* =========================
   * 멤버 강퇴 (DELETE)
   * ========================= */
  const handleRemove = async (calendarMemberId) => {
    if (!window.confirm("정말 제거하시겠습니까?")) return;

    try {
      await ApiService.removeCalendarMember(calendarMemberId);
      fetchMembers();
    } catch (e) {
      console.error("멤버 제거 실패", e);
    }
  };

   if (membersLoading) return <p>불러오는 중...</p>;

  return (
    <div style={{ maxWidth: 720 }}>
      <h1 style={{ fontSize: "1.5rem", fontWeight: 700 }}>
        캘린더 멤버 관리
      </h1>

     {/* ===== 현재 멤버 ===== */}
      <section style={{ marginTop: "2rem" }}>
        <h2 style={{ fontSize: "1.1rem", fontWeight: 600 }}>현재 멤버</h2>

        {members.length === 0 ? (
          <EmptyBox
            title="아직 초대된 멤버가 없습니다"
            description="아래에서 회원을 검색해 멤버를 초대해보세요."
          />
        ) : (
          <div style={{ marginTop: "1rem", display: "grid", gap: 10 }}>
        {members.map((m) => (
  <MemberRow
    key={m.calendarMemberId}
    member={m}
    myId={myId}
    ownerId={ownerId}
    onRemove={() =>
      ApiService.removeCalendarMember(m.calendarMemberId)
        .then(fetchMembers)
    }
  />
))}

          </div>
        )}
      </section>

      {/* ===== 친구 목록 초대 ===== */}
<section style={{ marginTop: "3rem" }}>
  <h2 style={{ fontSize: "1.1rem", fontWeight: 600 }}>
    친구 목록에서 초대
  </h2>

  {friendsLoading ? (
    <p>친구 목록 불러오는 중...</p>
  ) : friends.length === 0 ? (
    <EmptyBox
      title="친구가 없습니다"
      description="먼저 친구를 추가해보세요."
    />
  ) : (
    <div style={{ marginTop: "1rem", display: "grid", gap: 8 }}>
      {friends.map((f) => {
        const alreadyMember = memberIdSet.has(f.memberId);
        const checked = selectedMemberIds.includes(f.memberId);

        return (
          <label
            key={f.memberId}
            style={{
              display: "flex",
              gap: 10,
              padding: "12px",
              border: "1px solid #e5e7eb",
              borderRadius: 12,
              background: alreadyMember ? "#f9fafb" : "white",
              opacity: alreadyMember ? 0.6 : 1,
              cursor: alreadyMember ? "not-allowed" : "pointer",
            }}
          >
            <input
              type="checkbox"
              disabled={alreadyMember}
              checked={checked}
              onChange={() => toggleSelect(f.memberId)}
            />

            <div>
              <div style={{ fontWeight: 700 }}>
                {f.friendName}
                {alreadyMember && (
                  <span style={{ marginLeft: 8, fontSize: 12, color: "#6b7280" }}>
                    (이미 멤버)
                  </span>
                )}
              </div>

              <div style={{ fontSize: 12, color: "#6b7280" }}>
                친구
              </div>
            </div>
          </label>
        );
      })}
    </div>
  )}
</section>


      {/* ===== 초대 영역 ===== */}
      <section style={{ marginTop: "3rem" }}>
        <h2 style={{ fontSize: "1.1rem", fontWeight: 600 }}>
          멤버 초대 (회원 검색)
        </h2>

        <div style={{ marginTop: "1rem" }}>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => handleSearch(e.target.value)}
            placeholder="이름 / 이메일 / 전화번호로 검색"
            style={{
              width: "100%",
              padding: "10px 12px",
              border: "1px solid #d1d5db",
              borderRadius: 10,
              fontSize: "0.95rem",
            }}
          />
        </div>

        {/* 검색 결과 */}
        <div style={{ marginTop: "1rem" }}>
          {searchLoading ? (
            <EmptyBox
              title="검색 중..."
              description="검색 결과를 불러오고 있어요."
            />
          ) : searchResults.length > 0 ? (
            <div style={{ display: "grid", gap: 8 }}>
              {searchResults.map((u) => {
                const alreadyMember = memberIdSet.has(u.memberId);
                const checked = selectedMemberIds.includes(u.memberId);

                return (
                  <label
                    key={u.memberId}
                    style={{
                      display: "flex",
                      gap: 10,
                      alignItems: "flex-start",
                      padding: "12px",
                      border: "1px solid #e5e7eb",
                      borderRadius: 12,
                      opacity: alreadyMember ? 0.6 : 1,
                      background: alreadyMember ? "#f9fafb" : "white",
                      cursor: alreadyMember ? "not-allowed" : "pointer",
                    }}
                  >
                    <input
                      type="checkbox"
                      disabled={alreadyMember}
                      checked={checked}
                      onChange={() => toggleSelect(u.memberId)}
                      style={{ marginTop: 2 }}
                    />

                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 700, color: "#111827" }}>
                        {u.name}
                        {alreadyMember && (
                          <span style={{ marginLeft: 8, fontSize: 12, color: "#6b7280" }}>
                            (이미 멤버)
                          </span>
                        )}
                      </div>
                      <div style={{ fontSize: 13, color: "#6b7280", marginTop: 2 }}>
                        {u.email || "-"}
                      </div>
                      {u.phoneNumber && (
                        <div style={{ fontSize: 12, color: "#9ca3af", marginTop: 2 }}>
                          {u.phoneNumber}
                        </div>
                      )}
                    </div>
                  </label>
                );
              })}
            </div>
          ) : searchQuery.trim() ? (
            <EmptyBox
              title="검색 결과가 없습니다"
              description="다른 키워드로 검색해보세요."
            />
          ) : (
            <EmptyBox
              title="회원을 검색하세요"
              description="이름 / 이메일 / 전화번호로 검색할 수 있어요."
            />
          )}
        </div>

        {/* 초대 버튼 */}
        <div style={{ marginTop: "1rem", display: "flex", gap: 8 }}>
          <button
            onClick={handleInvite}
            disabled={inviteLoading || selectedMemberIds.length === 0}
            style={{
              padding: "10px 14px",
              borderRadius: 10,
              border: "none",
              background: inviteLoading || selectedMemberIds.length === 0 ? "#93c5fd" : "#2563eb",
              color: "white",
              cursor: inviteLoading || selectedMemberIds.length === 0 ? "not-allowed" : "pointer",
              fontWeight: 600,
            }}
          >
            {inviteLoading ? "초대 중..." : `선택한 멤버 초대 (${selectedMemberIds.length})`}
          </button>

          <button
            onClick={clearSelection}
            disabled={inviteLoading || selectedMemberIds.length === 0}
            style={{
              padding: "10px 14px",
              borderRadius: 10,
              border: "1px solid #d1d5db",
              background: "white",
              cursor: inviteLoading || selectedMemberIds.length === 0 ? "not-allowed" : "pointer",
              fontWeight: 600,
            }}
          >
            선택 해제
          </button>
        </div>
      </section>

      <div style={{ marginTop: "3rem" }}>
        <button onClick={() => navigate(-1)}>← 돌아가기</button>
      </div>
    </div>
  );
};



/* =========================
 * Sub Components
 * ========================= */

const MemberRow = ({ member, myId, ownerId, onRemove }) => {
  const my = Number(myId);
  const owner = Number(ownerId);
  const memberId = Number(member.memberId);

  const isMe = memberId === my;
  const isOwner = memberId === owner;
  const amIOwner = my === owner;

  const showKickButton = amIOwner && !isMe;   // 내가 소유주 + 다른 사람
  const showLeaveButton = !amIOwner && isMe; // 내가 일반 멤버 + 나
console.log({
  memberId: member.memberId,
  myId,
  ownerId,
  isMe,
  isOwner,
  amIOwner,
});
  return (
    <div style={rowStyle}>
      <div>
        <strong>
          {member.memberName}
          {isMe && <span style={tagStyle}>(ME)</span>}
          {isOwner && <span style={tagStyle}>👑</span>}
        </strong>
        <div style={{ fontSize: 12, color: "#6b7280" }}>
          상태: {member.status}
        </div>
      </div>

      <div>
        {showKickButton && (
          <button onClick={onRemove} style={dangerBtn}>
            강퇴
          </button>
        )}

        {showLeaveButton && (
          <button onClick={onRemove} style={dangerBtn}>
            탈퇴
          </button>
        )}
      </div>
    </div>
  );
};


const actionBtn = (bg) => ({
  padding: "6px 10px",
  fontSize: 12,
  fontWeight: 600,
  borderRadius: 8,
  border: "none",
  background: bg,
  color: "white",
  cursor: "pointer",
});

/* =========================
 * MemberRow Styles
 * ========================= */
const rowStyle = {
  padding: "12px 14px",
  border: "1px solid #e5e7eb",
  borderRadius: 12,
  background: "white",
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 12,
};

const tagStyle = {
  marginLeft: 6,
  fontSize: 12,
  color: "#6b7280",
};

const dangerBtn = {
  padding: "6px 10px",
  fontSize: 12,
  fontWeight: 600,
  borderRadius: 8,
  border: "none",
  background: "#ef4444",
  color: "white",
  cursor: "pointer",
};



/* =========================
 * Empty UI Component
 * ========================= */
const EmptyBox = ({ title, description }) => (
  <div
    style={{
      marginTop: "1rem",
      padding: "2rem",
      border: "1px dashed #d1d5db",
      borderRadius: 12,
      textAlign: "center",
      color: "#6b7280",
    }}
  >
    <div style={{ fontWeight: 600, marginBottom: 6 }}>
      {title}
    </div>
    <div style={{ fontSize: "0.9rem" }}>
      {description}
    </div>
  </div>
);

const formatDate = (iso) => {
  if (!iso) return "-";
  try {
    return new Date(iso).toLocaleString("ko-KR");
  } catch {
    return "-";
  }
};
