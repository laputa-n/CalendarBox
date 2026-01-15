import { useEffect, useState } from 'react';
import { ApiService } from '../services/apiService'; // 🔥 핵심 수정

export const useInvitedCalendars = () => {
  const [invites, setInvites] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchInvites = async () => {
    try {
      setLoading(true);
      const res = await ApiService.getInvitedCalendars({ page: 0, size: 20 });
      setInvites(res.data.content ?? res.data.data?.content ?? []);
    } catch (e) {
      console.error('초대 목록 조회 실패', e);
    } finally {
      setLoading(false);
    }
  };

  const respondInvite = async (calendarMemberId, action) => {
    await ApiService.respondCalendarInvite(calendarMemberId, action);
    setInvites((prev) =>
      prev.filter((i) => i.calendarMemberId !== calendarMemberId)
    );
  };

  useEffect(() => {
    fetchInvites();
  }, []);

  return {
    invites,
    loading,
    respondInvite,
    refetch: fetchInvites,
  };
};
