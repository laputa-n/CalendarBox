// src/contexts/CalendarContext.js

import React, { createContext, useContext, useState, useEffect } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useError } from './ErrorContext';

const CalendarContext = createContext();

export const CalendarProvider = ({ children }) => {
  const [calendars, setCalendars] = useState([]);
  const [currentCalendar, setCurrentCalendar] = useState(null);
  const [loading, setLoading] = useState(false);
  const [sharedCalendars, setSharedCalendars] = useState([]);
  const [calendarMembers, setCalendarMembers] = useState([]);
  const { user, isAuthenticated } = useAuth();
  const { showError } = useError();

  // ✅ 오커런스 상태
  const [occurrencesByDay, setOccurrencesByDay] = useState({});

  useEffect(() => {
    if (isAuthenticated && user) {
      fetchCalendars();
    }
  }, [isAuthenticated, user]);

  /**
   * ✅ 캘린더 발생(Occurrences) 조회
   * 백엔드 응답 예
   * {
   *   calendarId: 1,
   *   fromUtc: "...",
   *   toUtc: "...",
   *   days: {
   *     "2025-11-01": [ {occ}, {occ} ],
   *     "2025-11-02": [ {occ} ]
   *   }
   * }
   */
  const fetchOccurrences = async ({ fromKst, toKst, calendarId }) => {
    try {
      console.log('[fetchOccurrences] params:', { fromKst, toKst, calendarId });

      const res = await ApiService.getCalendarOccurrences(calendarId, {
        fromKst,
        toKst,
      });

      const root = res?.data ?? res;
      console.log('[fetchOccurrences] raw root:', root);

      const daysObj =
        root?.days ??
        root?.data?.days ??
        {};

      if (!daysObj || typeof daysObj !== 'object') {
        console.warn('[fetchOccurrences] 예상과 다른 응답 형태:', root);
        setOccurrencesByDay({});
        return;
      }

      // normalize
      const normalized = {};
      Object.entries(daysObj).forEach(([dateStr, list]) => {
        console.log(`📌 ${dateStr} occurrences:`, list);
        if (!Array.isArray(list)) return;

        normalized[dateStr] = list.map((occ) => ({
          
          ...occ,
          date: occ.date || dateStr,
        }));
      });

      setOccurrencesByDay(normalized);
    } catch (e) {
      console.error('❌ [fetchOccurrences] 실패:', e);
    }
  };

  // ---------- 캘린더 CRUD ----------
  const transformCalendarData = (backendCalendar) => ({
    id: backendCalendar.calendarId,
    calendarId: backendCalendar.calendarId,
    ownerId: backendCalendar.ownerId,
    name: backendCalendar.name,
    description: backendCalendar.description || '',
    color: backendCalendar.color || '#3b82f6',
    type: backendCalendar.type || 'PERSONAL',
    visibility: backendCalendar.visibility || 'PRIVATE',
    isShared: backendCalendar.visibility === 'PUBLIC',
    createdAt: backendCalendar.createdAt,
    updatedAt: backendCalendar.updatedAt,
  });

  const handleApiError = (error, defaultMessage) => {
    console.error('API Error:', error);
    if (error.response) {
      const { code, message } = error.response;
      switch (code) {
        case 400:
          showError('잘못된 요청입니다.');
          break;
        case 403:
          showError('해당 캘린더에 접근할 권한이 없습니다.');
          break;
        case 500:
          showError('서버 오류가 발생했습니다.');
          break;
        default:
          showError(message || defaultMessage);
      }
    } else {
      showError(error.message || defaultMessage);
    }
  };

  const fetchCalendars = async () => {
    if (!isAuthenticated || !user) return;

    try {
      setLoading(true);
      const response = await ApiService.getCalendars();

      const calendarsArray =
        response?.data?.content || response?.data || response || [];

      const transformed = calendarsArray.map(transformCalendarData);
      setCalendars(transformed);

      if (transformed.length > 0 && !currentCalendar) {
        setCurrentCalendar(transformed[0]);
      }
    } catch (error) {
      handleApiError(error, '캘린더 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const createCalendar = async (calendarData) => {
    try {
      setLoading(true);
      const backendData = {
        name: calendarData.name,
        type: calendarData.type || 'PERSONAL',
        visibility: calendarData.visibility || 'PRIVATE',
        isDefault: calendarData.isDefault ?? false,
      };
      const response = await ApiService.createCalendar(backendData);
      await fetchCalendars();
      return response;
    } catch (error) {
      handleApiError(error, '캘린더 생성에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const updateCalendar = async (calendarId, calendarData) => {
    try {
      setLoading(true);
      const backendData = {
        name: calendarData.name,
        visibility: calendarData.visibility || 'PRIVATE',
      };
      const response = await ApiService.updateCalendar(calendarId, backendData);

      if (currentCalendar?.calendarId === calendarId) {
        const updatedCalendar = transformCalendarData({
          ...response,
          calendarId,
          ownerId: currentCalendar.ownerId,
          createdAt: currentCalendar.createdAt,
          updatedAt: new Date().toISOString(),
        });
        setCurrentCalendar(updatedCalendar);
      }

      await fetchCalendars();
      return response;
    } catch (error) {
      handleApiError(error, '캘린더 수정에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const deleteCalendar = async (calendarId) => {
    try {
      setLoading(true);
      await ApiService.deleteCalendar(calendarId);

      if (currentCalendar?.calendarId === calendarId) {
        const remaining = calendars.filter(
          (cal) => cal.calendarId !== calendarId
        );
        setCurrentCalendar(remaining.length > 0 ? remaining[0] : null);
      }

      await fetchCalendars();
    } catch (error) {
      handleApiError(error, '캘린더 삭제에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const getCalendarById = async (calendarId) => {
    try {
      const response = await ApiService.getCalendarById(calendarId);
      return transformCalendarData(response);
    } catch (error) {
      handleApiError(error, '캘린더 정보 조회에 실패했습니다.');
      throw error;
    }
  };

  const fetchCalendarMembers = async (calendarId, { page = 0, size = 10, status, sort } = {}) => {
  try {
    const res = await ApiService.getCalendarMembers(calendarId, { page, size, status, sort });

    const root = res?.data ?? res;
    const data = root?.data ?? root;

    const content = data?.content ?? [];
    setCalendarMembers(content);
    return content;
  } catch (error) {
    console.error('Failed to fetch calendar members:', error);
    return [];
  }
};

const inviteCalendarMembers = async (calendarId, memberIds) => {
  try {
    setLoading(true);
    const res = await ApiService.inviteCalendarMembers(calendarId, memberIds);

    // 초대 후 멤버 목록 갱신(선택)
    await fetchCalendarMembers(calendarId, { page: 0, size: 10 });

    return res;
  } catch (error) {
    handleApiError(error, '멤버 초대에 실패했습니다.');
    throw error;
  } finally {
    setLoading(false);
  }
};


  const respondToCalendarInvite = async (calendarMemberId, status) => {
    try {
      setLoading(true);
      const response = await ApiService.respondToCalendarInvite(
        calendarMemberId,
        status
      );
      await fetchCalendars();
      return response;
    } catch (error) {
      handleApiError(error, '초대 응답에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const removeCalendarMember = async (calendarMemberId) => {
    try {
      setLoading(true);
      await ApiService.removeCalendarMember(calendarMemberId);
      if (currentCalendar && calendarMembers.length > 0) {
        await fetchCalendarMembers(currentCalendar.calendarId);
      }
    } catch (error) {
      handleApiError(error, '멤버 제거에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const getCalendarScheduleCount = async (calendarId) => {
    try {
      const response = await ApiService.getCalendarSchedules(calendarId);
      return response.count || 0;
    } catch (error) {
      console.error('Failed to get schedule count:', error);
      return 0;
    }
  };

  const switchCalendar = (calendar) => {
    setCurrentCalendar(calendar);
  };

  const fetchSharedCalendars = async () => {
    try {
      setLoading(true);
      const allCalendars = await ApiService.getCalendars();
      const shared = allCalendars.filter((cal) => cal.visibility === 'PUBLIC');
      setSharedCalendars(shared.map(transformCalendarData));
    } catch (error) {
      handleApiError(error, '공유 캘린더 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const shareCalendar = async (calendarId, shareData) => {
    return updateCalendar(calendarId, {
      ...shareData,
      visibility: 'PUBLIC',
    });
  };

  const contextValue = {
    calendars,
    currentCalendar,
    setCurrentCalendar: switchCalendar,

    sharedCalendars,
    calendarMembers,
    loading,

    // ✨ occurrences exposed
    occurrencesByDay,
    fetchOccurrences,

    // calendar CRUD
    createCalendar,
    updateCalendar,
    deleteCalendar,
    getCalendarById,

    // members
    inviteCalendarMembers,
    fetchCalendarMembers,
    respondToCalendarInvite,
    removeCalendarMember,

    fetchSharedCalendars,
    getCalendarScheduleCount,
    shareCalendar,

    refreshCalendars: fetchCalendars,
  };

  return (
    <CalendarContext.Provider value={contextValue}>
      {children}
    </CalendarContext.Provider>
  );
};

export const useCalendars = () => {
  const context = useContext(CalendarContext);
  if (!context) {
    throw new Error('useCalendars must be used within a CalendarProvider');
  }
  return context;
};
