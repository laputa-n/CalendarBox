// src/contexts/ScheduleContext.js
import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useCalendars } from './CalendarContext';
import { useError } from './ErrorContext';
import { COLOR_TO_THEME, THEME_TO_COLOR } from '../utils/colorUtils';

const ScheduleContext = createContext();

export const ScheduleProvider = ({ children }) => {
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(false);

  const [scheduleDetail, setScheduleDetail] = useState(null);
  const [scheduleDetailLoading, setScheduleDetailLoading] = useState(false);

  const { currentCalendar } = useCalendars();
  const { user } = useAuth();
  const { showError } = useError();

  /** =========================
   * Helper: 백엔드 → 프론트
   * ========================= */
  const transformScheduleData = (item) => ({
    id: item.scheduleId || item.id,
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
    color: THEME_TO_COLOR[item.theme] || '#3b82f6',
  });

  /** =========================
   * 일정 목록 조회
   * ========================= */
  const fetchSchedules = useCallback(async (params = {}) => {
    if (!currentCalendar?.id || !user) return;

    try {
      setLoading(true);

      const now = new Date();
      const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString();
      const to = new Date(now.getFullYear(), now.getMonth() + 1, 1).toISOString();

      const res = await ApiService.getSchedules({
        calendarId: currentCalendar.id,
        from,
        to,
        ...params,
      });

      const raw = res.data?.content || [];
      setSchedules(raw.map(transformScheduleData));
    } catch (e) {
      showError('일정 조회 실패');
    } finally {
      setLoading(false);
    }
  }, [currentCalendar, user]);

  /** =========================
   * 🔥 일정 상세 조회
   * ========================= */
  const fetchScheduleDetail = useCallback(async (scheduleId) => {
    if (!scheduleId) return;

    try {
      // 🔥 이전 일정 상세 제거 (섞임 방지)
      setScheduleDetail(null);
      setScheduleDetailLoading(true);

      const res = await ApiService.getScheduleDetail(scheduleId);
      const data = res?.data;

      setScheduleDetail({
        id: data.scheduleId,
        calendarId: data.calendarId,
        title: data.title,
        memo: data.memo,
        theme: data.theme,
        color: THEME_TO_COLOR[data.theme] || '#3b82f6',
        startAt: data.startAt,
        endAt: data.endAt,
        createdAt: data.createdAt,
        updatedAt: data.updatedAt,
        summary: data.summary,
      });
    } catch (e) {
      showError('일정 상세 조회 실패');
    } finally {
      setScheduleDetailLoading(false);
    }
  }, []);

  /** =========================
   * 🔥 일정 상세 초기화 (모달 닫을 때 사용)
   * ========================= */
  const clearScheduleDetail = useCallback(() => {
    setScheduleDetail(null);
  }, []);

  /** =========================
   * 일정 생성
   * ========================= */
  const createSchedule = async (scheduleData) => {
    try {
      setLoading(true);

      const apiData = {
        title: scheduleData.title,
        memo: scheduleData.memo || scheduleData.description || '',
        theme: COLOR_TO_THEME[scheduleData.color] || 'BLUE',
        startAt: new Date(scheduleData.startAt || scheduleData.startDateTime).toISOString(),
        endAt: new Date(scheduleData.endAt || scheduleData.endDateTime).toISOString(),
      };

      const res = await ApiService.createSchedule(currentCalendar.id, apiData);
      await fetchSchedules();
      return res;
    } catch (e) {
      showError('일정 생성 실패');
      throw e;
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

      if (scheduleData.color) {
        apiData.theme = COLOR_TO_THEME[scheduleData.color] || 'BLUE';
      }

      if (scheduleData.startAt || scheduleData.startDateTime) {
        apiData.startAt = new Date(
          scheduleData.startAt || scheduleData.startDateTime
        ).toISOString();
      }

      if (scheduleData.endAt || scheduleData.endDateTime) {
        apiData.endAt = new Date(
          scheduleData.endAt || scheduleData.endDateTime
        ).toISOString();
      }

      await ApiService.patchSchedule(scheduleId, apiData);
      await fetchSchedules();
    } catch (e) {
      showError('일정 수정 실패');
    } finally {
      setLoading(false);
    }
  };

  /** =========================
   * 캘린더 변경 시 자동 갱신
   * ========================= */
  useEffect(() => {
    if (currentCalendar?.id) {
      fetchSchedules();
      clearScheduleDetail(); // 🔥 캘린더 바뀌면 상세 초기화
    }
  }, [currentCalendar]);

  /** =========================
   * Context 제공
   * ========================= */
  const contextValue = {
    schedules,
    loading,

    fetchSchedules,
    createSchedule,
    updateSchedule,

    // 🔥 상세
    scheduleDetail,
    scheduleDetailLoading,
    fetchScheduleDetail,
    clearScheduleDetail,
  };

  return (
    <ScheduleContext.Provider value={contextValue}>
      {children}
    </ScheduleContext.Provider>
  );
};

export const useSchedules = () => {
  const ctx = useContext(ScheduleContext);
  if (!ctx) throw new Error('useSchedules must be used within ScheduleProvider');
  return ctx;
};
