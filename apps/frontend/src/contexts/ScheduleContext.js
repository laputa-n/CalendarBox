// src/contexts/ScheduleContext.js
import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useCalendars } from './CalendarContext';
import { useError } from './ErrorContext';

const ScheduleContext = createContext();

export const ScheduleProvider = ({ children }) => {
  const [schedules, setSchedules] = useState([]);
  const [currentSchedule, setCurrentSchedule] = useState(null);
  const [loading, setLoading] = useState(false);
  const [participants, setParticipants] = useState([]);
  const [participantsLoading, setParticipantsLoading] = useState(false);
  const { currentCalendar } = useCalendars();
  const { user } = useAuth();
  const { showError } = useError();

  /** =========================
   * Helper: 백엔드 → 프론트 변환
   * ========================= */
  const transformScheduleData = (item) => ({
    id: item.scheduleId || item.id, // ✅ 둘 다 지원
    calendarId: item.calendarId,
    calendarType: item.calendarType,
    calendarName: item.calendarName,
    title: item.scheduleTitle || item.title,
    memo: item.memo || '',
    theme: item.theme || 'BLUE',
    startAt: item.startAt,
    endAt: item.endAt,
    startDateTime: item.startAt,
    endDateTime: item.endAt,
    color: item.theme?.toLowerCase?.() || '#3b82f6',
  });

  /** =========================
   * 일정 조회 (현재 캘린더 기준)
   * ========================= */
  const fetchSchedules = useCallback(async (params = {}) => {
    if (!currentCalendar || !user) {
      console.warn("⚠️ currentCalendar 또는 user가 없습니다. 요청 중단");
      return;
    }

    try {
      setLoading(true);
      const now = new Date();
      const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString();
      const to = new Date(now.getFullYear(), now.getMonth() + 1, 1).toISOString();

      const queryParams = {
        from,
        to,
        calendarId: currentCalendar?.id ?? null,
        ...params,
      };

      const response = await ApiService.getSchedules(queryParams);
      const rawList = response.data?.content || response.data || [];

      const transformed = rawList.map(transformScheduleData);
      setSchedules(transformed);
      console.log("✅ [fetchSchedules] 완료:", transformed);
    } catch (error) {
      console.error("❌ [fetchSchedules] 실패:", error);
      showError(error.message || "일정 조회 실패");
    } finally {
      setLoading(false);
    }
  }, [currentCalendar, user, showError]);

  /** =========================
   * 일정 전체 조회 (관리용)
   * ========================= */
  const fetchAllSchedules = useCallback(async (params = {}) => {
    if (!user) return;

    try {
      setLoading(true);
      const now = new Date();
      const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString();
      const to = new Date(now.getFullYear(), now.getMonth() + 1, 1).toISOString();

      const safeParams = { from, to, ...params };
      const response = await ApiService.getSchedules(safeParams);
      const rawList =
        response.data?.content ||
        response.data?.scheduleList ||
        response.scheduleList ||
        response.data ||
        [];

      const transformed = rawList.map(transformScheduleData);
      setSchedules(transformed);
      console.log("✅ [fetchAllSchedules] 완료:", transformed);
    } catch (error) {
      console.error("❌ [fetchAllSchedules] 실패:", error);
      showError(error.message || "전체 일정 조회 실패");
    } finally {
      setLoading(false);
    }
  }, [user, showError]);

  /** =========================
   * 일정 생성
   * ========================= */
  const createSchedule = async (scheduleData) => {
    try {
      setLoading(true);

      const apiData = {
        title: scheduleData.title,
        memo: scheduleData.memo || scheduleData.description || "",
        theme:
          scheduleData.theme && scheduleData.theme.startsWith("#")
            ? "BLUE"
            : String(scheduleData.theme || "BLUE").toUpperCase(),
        startAt: new Date(scheduleData.startAt || scheduleData.startDateTime).toISOString(),
        endAt: new Date(scheduleData.endAt || scheduleData.endDateTime).toISOString(),
        links: scheduleData.links || [],
        places: scheduleData.places || [],
        participants: scheduleData.participants || [],
        todos: scheduleData.todos || [],
        reminders: scheduleData.reminders || [],
        recurrence: scheduleData.recurrence || { freq: 'DAILY', intervalCount: 1, byDay: [], until: '' }
      };

      const res = await ApiService.createSchedule(currentCalendar.id, apiData);
      await fetchAllSchedules(); // ✅ 생성 후 즉시 새로고침
      return res;
    } catch (error) {
      console.error("❌ [createSchedule] 실패:", error);
      showError(error.message || "일정 생성 실패");
      throw error;
    } finally {
      setLoading(false);
    }
  };

  /** =========================
   * 일정 수정
   * ========================= */
  const updateSchedule = async (scheduleId, scheduleData) => {
    try {
      setLoading(true);

      const apiData = {};
      if ('title' in scheduleData) apiData.title = scheduleData.title;
      if ('memo' in scheduleData || 'description' in scheduleData)
        apiData.memo = scheduleData.memo ?? scheduleData.description ?? '';

      const rawTheme = scheduleData.theme || scheduleData.color;
      if (rawTheme) {
        apiData.theme = rawTheme.startsWith('#')
          ? 'BLUE'
          : String(rawTheme).toUpperCase();
      }

      if (scheduleData.startAt || scheduleData.startDateTime) {
        const s = new Date(scheduleData.startAt || scheduleData.startDateTime);
        if (!isNaN(s)) apiData.startAt = s.toISOString();
      }

      if (scheduleData.endAt || scheduleData.endDateTime) {
        const e = new Date(scheduleData.endAt || scheduleData.endDateTime);
        if (!isNaN(e)) apiData.endAt = e.toISOString();
      }

      console.log('📤 [updateSchedule] 요청 페이로드:', apiData);

      await ApiService.patchSchedule(scheduleId, apiData);
      await fetchAllSchedules(); // ✅ 수정 후 전체 갱신
    } catch (error) {
      console.error('❌ [updateSchedule] 실패:', error);
      showError(error.message || '일정 수정 실패');
    } finally {
      setLoading(false);
    }
  };

  /** =========================
   * 일정 삭제
   * ========================= */
  const deleteSchedule = async (scheduleId) => {
    try {
      setLoading(true);
      await ApiService.deleteSchedule(scheduleId);
      if (currentSchedule?.scheduleId === scheduleId) setCurrentSchedule(null);
      await fetchAllSchedules(); // ✅ 삭제 후 전체 갱신
    } catch (error) {
      console.error("❌ [deleteSchedule] 실패:", error);
      showError(error.message || "일정 삭제 실패");
    } finally {
      setLoading(false);
    }
  };

const addScheduleParticipant = async (scheduleId, payload) => {
  return ApiService.addScheduleParticipant(scheduleId, payload);
};

const removeScheduleParticipant = async (scheduleId, participantId) => {
  return ApiService.removeScheduleParticipant(scheduleId, participantId);
};

const respondToScheduleInvite = async (scheduleId, participantId, action) => {
  return ApiService.respondToScheduleInvite(scheduleId, participantId, action);
};

/** =========================
 * 일정 참여자 목록 조회
 * ========================= */
const fetchScheduleParticipants = async (scheduleId) => {
  if (!scheduleId) return;

  try {
    setParticipantsLoading(true);
    const res = await ApiService.getScheduleParticipants(scheduleId);
    const list = res.data?.content || [];
    setParticipants(list);
  } catch (error) {
    console.error('❌ [fetchScheduleParticipants] 실패:', error);
    showError(error.message || '일정 참여자 조회 실패');
  } finally {
    setParticipantsLoading(false);
  }
};


  /** =========================
   * 캘린더 변경 감지 → 자동 새로고침
   * ========================= */
  useEffect(() => {
    if (currentCalendar?.id) {
      fetchSchedules();
    }
  }, [currentCalendar, fetchSchedules]);

  /** =========================
   * Context 반환
   * ========================= */
  const contextValue = {
    schedules,
    currentSchedule,
    setCurrentSchedule,
    loading,
    createSchedule,
    updateSchedule,
    deleteSchedule,
    fetchSchedules,
    fetchAllSchedules,
    participants,
  participantsLoading,
  fetchScheduleParticipants,
  };

  return (
    <ScheduleContext.Provider value={contextValue}>
      {children}
    </ScheduleContext.Provider>
  );
};

export const useSchedules = () => {
  const context = useContext(ScheduleContext);
  if (!context) throw new Error("useSchedules must be used within a ScheduleProvider");
  return context;
};
