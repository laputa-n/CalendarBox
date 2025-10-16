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

  const { currentCalendar } = useCalendars();
  const { user } = useAuth();
  const { showError } = useError();

  /** =========================
   * Helper: 백엔드 → 프론트 변환
   * ========================= */
  const transformScheduleData = (item) => ({
    id: item.scheduleId,
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
    console.log("📡 [fetchSchedules] 실행됨:", {
      calendarId: currentCalendar?.id,
      userId: user?.id,
    });

    if (!currentCalendar || !user) {
      console.warn("⚠️ currentCalendar 또는 user가 없습니다. 요청 중단");
      return;
    }

    try {
      setLoading(true);

      // ✅ 이번 달 1일 ~ 다음 달 1일
      const now = new Date();
      const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
      const startOfNextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1, 0, 0, 0);
      const from = startOfMonth.toISOString();
      const to = startOfNextMonth.toISOString();

      const queryParams = {
        from,
        to,
        calendarId: currentCalendar?.id ?? null,
        ...params,
      };

      console.log("📤 [fetchSchedules] 요청 파라미터:", queryParams);

      const response = await ApiService.getSchedules(queryParams);
      console.log("📦 [fetchSchedules] 응답:", response.data);

      const scheduleList = response.data?.content || [];
      console.log("🧩 [fetchSchedules] 변환 전 리스트:", scheduleList);

      const transformed = scheduleList.map(transformScheduleData);
      console.log("✅ [fetchSchedules] 변환 후 리스트:", transformed);

      setSchedules(transformed);
    } catch (error) {
      console.error("❌ [fetchSchedules] 일정 조회 실패:", error);
      showError(error.message || "일정 조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }, [currentCalendar, user, showError]);

  /** =========================
   * 일정 전체 조회 (관리용)
   * ========================= */
  const fetchAllSchedules = useCallback(async (params = {}) => {
    if (!user) return;
    console.log("📡 [fetchAllSchedules] 전체 일정 조회 시작");

    try {
      setLoading(true);

      const now = new Date();
      const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0);
      const startOfNextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1, 0, 0, 0);
      const from = startOfMonth.toISOString();
      const to = startOfNextMonth.toISOString();

      const safeParams = {
        from,
        to,
        ...params,
      };

      console.log("📤 [fetchAllSchedules] 요청 파라미터:", safeParams);

      const response = await ApiService.getSchedules(safeParams);
      console.log("📦 [fetchAllSchedules] 응답:", response.data);

      const scheduleList =
        response.data?.content ||
        response.data?.scheduleList ||
        response.scheduleList ||
        response.data ||
        [];

      console.log("🧩 [fetchAllSchedules] 변환 전 리스트:", scheduleList);

      const transformed = scheduleList.map(transformScheduleData);
      console.log("✅ [fetchAllSchedules] 변환 후 리스트:", transformed);

      setSchedules(transformed);
    } catch (error) {
      console.error("❌ [fetchAllSchedules] 실패:", error);
      showError(error.message || "전체 일정 조회에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }, [user, showError]);

  /** =========================
   * 일정 생성
   * ========================= */
  const createSchedule = async (scheduleData) => {
    if (!currentCalendar?.id) {
      showError("캘린더를 선택해주세요.");
      return;
    }

    try {
      setLoading(true);
      const apiData = {
        title: scheduleData.title,
        memo: scheduleData.memo || scheduleData.description || "",
        theme:
          scheduleData.theme && scheduleData.theme.startsWith("#")
            ? "BLUE"
            : scheduleData.theme?.toUpperCase() || "BLUE",
        startAt: new Date(scheduleData.startAt || scheduleData.startDateTime).toISOString(),
        endAt: new Date(scheduleData.endAt || scheduleData.endDateTime).toISOString(),
        links: scheduleData.links || [],
        places: scheduleData.places || [],
        participants: scheduleData.participants || [],
        todos: scheduleData.todos || [],
        reminders: scheduleData.reminders || [],
        recurrence: scheduleData.recurrence || null,
      };

      console.log("📦 [createSchedule] 전송 데이터:", apiData);

      const response = await ApiService.createSchedule(currentCalendar.id, apiData);
      console.log("✅ [createSchedule] 응답:", response);

      await fetchSchedules(); // ✅ 즉시 DB 새로고침
    } catch (error) {
      console.error("❌ [createSchedule] 실패:", error);
      showError(error.message || "일정 생성에 실패했습니다.");
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
    if ('memo' in scheduleData || 'description' in scheduleData) {
      apiData.memo = scheduleData.memo ?? scheduleData.description ?? '';
    }

    // ✅ theme 변환 (ENUM 매칭)
    const rawTheme = scheduleData.theme || scheduleData.color;
    if (rawTheme) {
      apiData.theme = rawTheme.startsWith('#')
        ? 'BLUE' // 기본값으로 ENUM
        : String(rawTheme).toUpperCase();
    }

    // ✅ 날짜 변환 (UTC ISO)
    if (scheduleData.startAt || scheduleData.startDateTime) {
      const s = new Date(scheduleData.startAt || scheduleData.startDateTime);
      if (!isNaN(s)) apiData.startAt = s.toISOString();
    }
    if (scheduleData.endAt || scheduleData.endDateTime) {
      const e = new Date(scheduleData.endAt || scheduleData.endDateTime);
      if (!isNaN(e)) apiData.endAt = e.toISOString();
    }

    console.log('📤 [updateSchedule] 요청 페이로드:', apiData);

    // ✅ PATCH로 변경
    await ApiService.patchSchedule(scheduleId, apiData);
    await fetchSchedules();
  } catch (error) {
    console.error('❌ [updateSchedule] 실패:', error);
    showError(error.message || '일정 수정에 실패했습니다.');
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
      console.log("🗑 [deleteSchedule] 삭제 요청:", scheduleId);

      await ApiService.deleteSchedule(scheduleId);
      console.log("✅ [deleteSchedule] 성공:", scheduleId);

      if (currentSchedule?.scheduleId === scheduleId) setCurrentSchedule(null);
      await fetchSchedules();
    } catch (error) {
      console.error("❌ [deleteSchedule] 실패:", error);
      showError(error.message || "일정 삭제에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  /** =========================
   * 캘린더 변경 감지 → 자동 새로고침
   * ========================= */
  useEffect(() => {
    if (currentCalendar?.id) {
      console.log("🔄 [useEffect] 캘린더 변경 감지됨:", currentCalendar.id);
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
