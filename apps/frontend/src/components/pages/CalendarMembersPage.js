// src/pages/calendar/CalendarMembersPage.jsx
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { ApiService } from '../../services/apiService';
import { useFriends } from '../../contexts/FriendContext';

export const CalendarMembersPage = () => {
  const { calendarId } = useParams();

  const { friends, fetchFriends } = useFriends();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);

  // 🔥 초대 선택 상태
  const [selectedMemberIds, setSelectedMemberIds] = useState([]);

  /* =========================
   * 캘린더 멤버 조회
   * ========================= */
  useEffect(() => {
    const fetchMembers = async () => {
      try {
        const res = await ApiService.getCalendarMembers(calendarId);
        const data = res?.data?.data;
        setMembers(data?.content || []);
      } catch (e) {
        console.error('캘린더 멤버 조회 실패', e);
      } finally {
        setLoading(false);
      }
    };

    fetchMembers();
    fetchFriends(); // 친구 목록
  }, [calendarId]);


  const handleSearch = async (query) => {
  setSearchQuery(query);

  if (!query.trim()) {
    setSearchResults([]);
    return;
  }

  try {
    setSearchLoading(true);
    const res = await ApiService.searchMembers(query);
    setSearchResults(res?.data?.data?.content || []);
  } catch (e) {
    console.error('회원 검색 실패', e);
  } finally {
    setSearchLoading(false);
  }
};

  /* =========================
   * 체크박스 토글
   * ========================= */
  const toggleSelect = (memberId) => {
    setSelectedMemberIds(prev =>
      prev.includes(memberId)
        ? prev.filter(id => id !== memberId)
        : [...prev, memberId]
    );
  };

  /* =========================
   * 초대 요청
   * ========================= */
  const handleInvite = async () => {
    if (selectedMemberIds.length === 0) {
      alert('초대할 멤버를 선택하세요.');
      return;
    }

    try {
      await ApiService.inviteCalendarMembers(calendarId, selectedMemberIds);
      alert('초대가 완료되었습니다.');

      setSelectedMemberIds([]);
      // 멤버 목록 새로고침
      const res = await ApiService.getCalendarMembers(calendarId);
      setMembers(res?.data?.data?.content || []);
    } catch (e) {
      console.error('멤버 초대 실패', e);
      alert('초대에 실패했습니다.');
    }
  };

  if (loading) return <p>불러오는 중...</p>;

  /* =========================
   * Render
   * ========================= */
  return (
    <div style={{ padding: '2rem', maxWidth: 700 }}>
      <h2>캘린더 멤버 관리</h2>

      {/* =========================
          친구 목록에서 초대
      ========================= */}
      <h3 style={{ marginTop: '2rem' }}>회원 검색으로 초대</h3>

<input
  type="text"
  value={searchQuery}
  onChange={(e) => handleSearch(e.target.value)}
  placeholder="이름 / 이메일로 검색"
  style={{ width: '100%', padding: '8px' }}
/>

{searchLoading ? (
  <p>검색 중...</p>
) : searchResults.length > 0 ? (
  searchResults.map(member => (
    <label key={member.memberId} style={{ display: 'block', marginTop: 8 }}>
      <input
        type="checkbox"
        checked={selectedMemberIds.includes(member.memberId)}
        onChange={() => toggleSelect(member.memberId)}
      />
      {member.name} ({member.email})
    </label>
  ))
) : searchQuery ? (
  <p>검색 결과가 없습니다.</p>
) : (
  <p>회원을 검색하세요.</p>
)}

<button
  onClick={handleInvite}
  style={{ marginTop: '1rem' }}
>
  선택한 멤버 초대
</button>

    </div>
  );
};
