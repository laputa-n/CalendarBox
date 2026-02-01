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
  const [participants, setParticipants] = useState([]);
  const [participantsLoading, setParticipantsLoading] = useState(false);
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

    // ✅ 기본을 '전체 조회용' 넓은 기간으로 잡기
    const DEFAULT_FROM = '1970-01-01T00:00:00.000Z';
    const DEFAULT_TO   = '2100-01-01T00:00:00.000Z';

    const { from = DEFAULT_FROM, to = DEFAULT_TO, ...rest } = params;

    const res = await ApiService.getSchedules({
      calendarId: currentCalendar.id,
      from,
      to,
      ...rest,
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
    console.log('🧠 [Context] fetchScheduleDetail START, id =', scheduleId);

  try {
    setScheduleDetailLoading(true);

    const res = await ApiService.getScheduleDetail(scheduleId);
    const data = res?.data;
     console.log('🧠 [Context] fetchScheduleDetail RESPONSE =', data);
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

       console.log('🧠 [Context] fetchScheduleDetail SET 완료');
  } catch (e) {
    showError('일정 상세 조회 실패');
  } finally {
    setScheduleDetailLoading(false);
  }
}, []);


  const fetchScheduleParticipants = useCallback(async (scheduleId) => {
  if (!scheduleId) return;

  try {
    setParticipantsLoading(true);
    const res = await ApiService.getScheduleParticipants(scheduleId);
    setParticipants(res.data?.content || []);
  } catch (e) {
    showError('참여자 조회 실패');
  } finally {
    setParticipantsLoading(false);
  }
}, []);


  /** =========================
   * 🔥 일정 상세 초기화 (모달 닫을 때 사용)
   * ========================= */
  const clearScheduleDetail = useCallback(() => {
     console.log('🧹 [Context] clearScheduleDetail CALLED');
    setScheduleDetail(null);
  }, []);

  /** =========================
   * 일정 생성
   * ========================= */
// ✅ theme 결정 헬퍼(권장)
const pickTheme = (scheduleData) => {
  const ALLOWED = ['RED','BLUE','GREEN','YELLOW','PURPLE','PINK','BLACK','ORANGE'];

  // 1) scheduleData.theme가 이미 enum이면 그대로 사용
  if (scheduleData?.theme && ALLOWED.includes(String(scheduleData.theme))) {
    return String(scheduleData.theme);
  }

  // 2) scheduleData.color가 enum이면 그대로 사용 (혹시 color에 enum 넣는 화면도 대비)
  if (scheduleData?.color && ALLOWED.includes(String(scheduleData.color))) {
    return String(scheduleData.color);
  }

  // 3) scheduleData.color가 HEX면 COLOR_TO_THEME로 매핑
  if (scheduleData?.color && typeof scheduleData.color === 'string') {
    const t = COLOR_TO_THEME[scheduleData.color];
    if (t) return t;
  }

  return 'BLUE';
};

const createSchedule = async (scheduleData) => {
  try {
    setLoading(true);

    const apiData = {
      title: scheduleData.title,
      memo: scheduleData.memo || scheduleData.description || '',
      theme: pickTheme(scheduleData), // ✅ 여기!
      startAt: new Date(scheduleData.startAt || scheduleData.startDateTime).toISOString(),
      endAt: new Date(scheduleData.endAt || scheduleData.endDateTime).toISOString(),
    };

    console.log('✅ [Context createSchedule] scheduleData=', scheduleData);
    console.log('✅ [Context createSchedule] apiData=', apiData);

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

  const addScheduleParticipant = async (scheduleId, payload) => {
  return ApiService.addScheduleParticipant(scheduleId, payload);
};

const respondToScheduleInvite = async (scheduleId, participantId, action) => {
  return ApiService.respondToScheduleInvite(scheduleId, participantId, action);
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

    // ✅ theme/color 둘 다 대응
    if ('theme' in scheduleData || 'color' in scheduleData) {
      apiData.theme = pickTheme(scheduleData);
    }

    if (scheduleData.startAt || scheduleData.startDateTime) {
      apiData.startAt = new Date(scheduleData.startAt || scheduleData.startDateTime).toISOString();
    }
    if (scheduleData.endAt || scheduleData.endDateTime) {
      apiData.endAt = new Date(scheduleData.endAt || scheduleData.endDateTime).toISOString();
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
 * 일정 삭제
 * ========================= */
const deleteSchedule = async (scheduleId) => {
  if (!scheduleId) return;

  try {
    setLoading(true);

    // ApiService에 삭제 함수가 있다고 가정: deleteSchedule(scheduleId)
    await ApiService.deleteSchedule(scheduleId);

    // 목록 갱신
    await fetchSchedules();

    // 만약 삭제한 일정이 상세로 열려있으면 상세도 초기화
    if (scheduleDetail?.id === scheduleId) {
      clearScheduleDetail();
    }
  } catch (e) {
    showError('일정 삭제 실패');
    throw e;
  } finally {
    setLoading(false);
  }
};
/** =========================
 * 일정 검색
 * ========================= */
const searchSchedules = useCallback(async (keyword, params = {}) => {
  if (!currentCalendar?.id || !user) return;

  const q = String(keyword ?? '').trim();
  if (!q) {
    await fetchSchedules(); 
    return;
  }
  try {
    setLoading(true);
    const res = await ApiService.searchSchedules({
      calendarId: currentCalendar.id,
      query: q, 
      ...params, 
    });
    const raw = res?.data?.data?.content || res?.data?.content || [];
    setSchedules(raw.map(transformScheduleData));
  } catch (e) {
    showError('일정 검색 실패');
  } finally {
    setLoading(false);
  }
}, [currentCalendar, user, fetchSchedules]);

  /** =========================
   * 캘린더 변경 시 자동 갱신
   * ========================= */
  useEffect(() => {
    if (currentCalendar?.id) {
      fetchSchedules();
      clearScheduleDetail(); // 🔥 캘린더 바뀌면 상세 초기화
    }
  }, [currentCalendar]);

  useEffect(() => {
  console.log('📦 [Context] scheduleDetail CHANGED:', scheduleDetail);
}, [scheduleDetail]);

const contextValue = {
  schedules,
  loading,

  fetchSchedules,
  createSchedule,
  updateSchedule,
  deleteSchedule, // ✅ 추가
  searchSchedules,
  scheduleDetail,
  scheduleDetailLoading,
  fetchScheduleDetail,
  clearScheduleDetail,

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
  const ctx = useContext(ScheduleContext);
  if (!ctx) throw new Error('useSchedules must be used within ScheduleProvider');
  return ctx;
};
