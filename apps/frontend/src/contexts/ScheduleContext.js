// src/contexts/ScheduleContext.js
import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useCalendars } from './CalendarContext';
import { useError } from './ErrorContext';
import { COLOR_TO_THEME , THEME_TO_COLOR } from '../utils/colorUtils';

const ScheduleContext = createContext();

export const ScheduleProvider = ({ children }) => {
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(false);
  const [participants, setParticipants] = useState([]);
  const [participantsLoading, setParticipantsLoading] = useState(false);
  const { currentCalendar } = useCalendars();
  const { user } = useAuth();
  const { showError } = useError();
  const [scheduleDetail, setScheduleDetail] = useState(null);
const [scheduleDetailLoading, setScheduleDetailLoading] = useState(false);


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
    color: THEME_TO_COLOR[item.theme] || '#3b82f6',
  });

  /** =========================
   * 일정 조회 (현재 캘린더 기준)
   * ========================= */
 const fetchSchedules = useCallback(async (params = {}) => {
  if (!currentCalendar?.id || !user) return;

  try {
    setLoading(true);

    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString();
    const to = new Date(now.getFullYear(), now.getMonth() + 1, 1).toISOString();

    const response = await ApiService.getSchedules({
      calendarId: currentCalendar.id, // ✅ 무조건 포함
      from,
      to,
      ...params,
    });

    const raw = response.data?.content || [];
    setSchedules(raw.map(transformScheduleData));
  } catch (e) {
    showError('일정 조회 실패');
  } finally {
    setLoading(false);
  }
}, [currentCalendar, user]);



const fetchScheduleDetail = useCallback(async (scheduleId) => {
  if (!scheduleId) return;

  try {
    setScheduleDetailLoading(true);

    const res = await ApiService.getScheduleDetail(scheduleId);
    const data = res?.data?.data;

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
  links: scheduleData.links || [],
  places: scheduleData.places || [],
  todos: scheduleData.todos || [],
  reminders: scheduleData.reminders || [],
  ...(scheduleData.recurrence ? { recurrence: scheduleData.recurrence } : {}),
};   const res = await ApiService.createSchedule(currentCalendar.id, apiData);
      await fetchSchedules();// ✅ 생성 후 즉시 새로고침
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
      if (scheduleData.color) {
   apiData.theme = COLOR_TO_THEME[scheduleData.color] || 'BLUE';
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
      await fetchSchedules();// ✅ 수정 후 전체 갱신
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
      await fetchSchedules();// ✅ 삭제 후 전체 갱신
    } catch (error) {
      console.error("❌ [deleteSchedule] 실패:", error);
      showError(error.message || "일정 삭제 실패");
    } finally {
      setLoading(false);
    }
  };

const searchSchedules = async (query) => {
  await fetchSchedules({ query });
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
const fetchScheduleParticipants = useCallback(async (scheduleId) => {
  if (!scheduleId) return;

  try {
    setParticipantsLoading(true);
    const res = await ApiService.getScheduleParticipants(scheduleId);
    setParticipants(res.data?.content || []);
  } catch (error) {
    console.error('❌ [fetchScheduleParticipants] 실패:', error);
    showError(error.message || '일정 참여자 조회 실패');
  } finally {
    setParticipantsLoading(false);
  }
}, []);


  /** =========================
   * 캘린더 변경 감지 → 자동 새로고침
   * ========================= */
useEffect(() => {
  if (currentCalendar?.id) {
    fetchSchedules();
  }
}, [currentCalendar]); 

  /** =========================
   * Context 반환
   * ========================= */
 const contextValue = {
  schedules,
  loading,

  // 목록
  fetchSchedules,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  searchSchedules,

  // 🔥 상세
  scheduleDetail,
  scheduleDetailLoading,
  fetchScheduleDetail,

  // 참여자
  participants,
  participantsLoading,
  fetchScheduleParticipants,
  addScheduleParticipant,
  respondToScheduleInvite,
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
