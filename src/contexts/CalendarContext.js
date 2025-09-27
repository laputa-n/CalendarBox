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

  useEffect(() => {
    if (isAuthenticated && user) {
      fetchCalendars();
    }
  }, [isAuthenticated, user]);

  // 백엔드 데이터를 프론트엔드에서 사용하기 편한 형태로 변환
  const transformCalendarData = (backendCalendar) => {
    return {
      id: backendCalendar.calendarId, // UI에서 사용하는 id
      calendarId: backendCalendar.calendarId, // 백엔드 호출시 사용
      ownerId: backendCalendar.ownerId,
      name: backendCalendar.name,
      description: backendCalendar.description || '',
      color: backendCalendar.color || '#3b82f6',
      type: backendCalendar.type || 'PERSONAL',
      visibility: backendCalendar.visibility || 'PRIVATE',
      // 기존 UI 호환성을 위한 필드들
      isShared: backendCalendar.visibility === 'PUBLIC',
      createdAt: backendCalendar.createdAt,
      updatedAt: backendCalendar.updatedAt
    };
  };

  // API 명세 에러 처리 헬퍼
  const handleApiError = (error, defaultMessage) => {
    console.error('API Error:', error);
    
    // API 명세 에러 구조 처리
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
      const calendarData = await ApiService.getCalendars();
      
      // API 응답이 배열인지 확인하고 변환
      const calendarsArray = Array.isArray(calendarData) ? calendarData : [];
      const transformedCalendars = calendarsArray.map(transformCalendarData);
      
      setCalendars(transformedCalendars);
      
      // 현재 캘린더가 없으면 첫 번째 캘린더를 선택
      if (transformedCalendars.length > 0 && !currentCalendar) {
        setCurrentCalendar(transformedCalendars[0]);
      }
    } catch (error) {
      handleApiError(error, '캘린더 조회에 실패했습니다.');
      
      // 개발 시 목 데이터 사용 (API 명세 형식에 맞춘 응답 구조)
      const mockResponse = {
        code: 200,
        status: "OK",
        message: "캘린더 목록 조회 성공",
        data: [
          {
            calendarId: 1,
            ownerId: user?.id || 1,
            name: '개인 캘린더',
            description: '나의 개인 일정',
            color: '#3b82f6',
            type: 'PERSONAL',
            visibility: 'PRIVATE',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          }
        ]
      };
      
      const transformedMockCalendars = mockResponse.data.map(transformCalendarData);
      setCalendars(transformedMockCalendars);
      setCurrentCalendar(transformedMockCalendars[0]);
    } finally {
      setLoading(false);
    }
  };

  const createCalendar = async (calendarData) => {
    try {
      setLoading(true);
      
      // UI 데이터를 백엔드 형식으로 변환
      const backendData = {
        name: calendarData.name,
        type: calendarData.type || 'PERSONAL',
        visibility: calendarData.visibility || 'PRIVATE'
      };
      
      const response = await ApiService.createCalendar(backendData);
      await fetchCalendars(); // 목록 새로고침
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
      
      // UI 데이터를 백엔드 형식으로 변환
      const backendData = {
        name: calendarData.name,
        visibility: calendarData.visibility || 'PRIVATE'
      };
      
      const response = await ApiService.updateCalendar(calendarId, backendData);
      
      // 현재 선택된 캘린더가 수정된 캘린더인 경우 업데이트
      if (currentCalendar?.calendarId === calendarId) {
        const updatedCalendar = transformCalendarData({
          ...response,
          calendarId,
          ownerId: currentCalendar.ownerId,
          createdAt: currentCalendar.createdAt,
          updatedAt: new Date().toISOString()
        });
        setCurrentCalendar(updatedCalendar);
      }
      
      await fetchCalendars(); // 목록 새로고침
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
      
      // 삭제된 캘린더가 현재 선택된 캘린더인 경우 처리
      if (currentCalendar?.calendarId === calendarId) {
        const remainingCalendars = calendars.filter(cal => cal.calendarId !== calendarId);
        setCurrentCalendar(remainingCalendars.length > 0 ? remainingCalendars[0] : null);
      }
      
      await fetchCalendars(); // 목록 새로고침
    } catch (error) {
      handleApiError(error, '캘린더 삭제에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 캘린더 상세 정보 조회
  const getCalendarById = async (calendarId) => {
    try {
      const response = await ApiService.getCalendarById(calendarId);
      return transformCalendarData(response);
    } catch (error) {
      handleApiError(error, '캘린더 정보 조회에 실패했습니다.');
      throw error;
    }
  };

  // === 캘린더 멤버 관리 ===

  // 캘린더 멤버 초대
  const inviteCalendarMembers = async (calendarId, memberIds) => {
    try {
      setLoading(true);
      const response = await ApiService.inviteCalendarMembers(calendarId, memberIds);
      await fetchCalendarMembers(calendarId); // 멤버 목록 새로고침
      return response;
    } catch (error) {
      handleApiError(error, '멤버 초대에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 캘린더 멤버 목록 조회
  const fetchCalendarMembers = async (calendarId, page = 1, size = 10) => {
    try {
      const response = await ApiService.getCalendarMembers(calendarId, page, size);
      const memberList = Array.isArray(response) ? response : response.content || [];
      setCalendarMembers(memberList);
      return memberList;
    } catch (error) {
      console.error('Failed to fetch calendar members:', error);
      return [];
    }
  };

  // 캘린더 초대 응답 (수락/거절)
  const respondToCalendarInvite = async (calendarMemberId, status) => {
    try {
      setLoading(true);
      // status: 'ACCEPTED' or 'REJECTED'
      const response = await ApiService.respondToCalendarInvite(calendarMemberId, status);
      await fetchCalendars(); // 내 캘린더 목록 새로고침
      return response;
    } catch (error) {
      handleApiError(error, '초대 응답에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 캘린더 멤버 제거/탈퇴
  const removeCalendarMember = async (calendarMemberId) => {
    try {
      setLoading(true);
      await ApiService.removeCalendarMember(calendarMemberId);
      // 현재 캘린더의 멤버 목록이 있다면 새로고침
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

  // === 캘린더 관련 유틸리티 ===

  // 특정 캘린더의 일정 개수 조회 (통계용)
  const getCalendarScheduleCount = async (calendarId) => {
    try {
      const response = await ApiService.getCalendarSchedules(calendarId);
      return response.count || 0;
    } catch (error) {
      console.error('Failed to get schedule count:', error);
      return 0;
    }
  };

  // 캘린더 전환 (ScheduleContext와 연동을 위한 헬퍼)
  const switchCalendar = (calendar) => {
    setCurrentCalendar(calendar);
    // ScheduleContext에서 useEffect로 currentCalendar 변경을 감지하여 
    // 해당 캘린더의 일정을 자동으로 불러옴
  };

  // 공유 캘린더 목록 조회 (visibility가 PUBLIC인 캘린더들)
  const fetchSharedCalendars = async () => {
    try {
      setLoading(true);
      const allCalendars = await ApiService.getCalendars();
      const shared = allCalendars.filter(cal => cal.visibility === 'PUBLIC');
      setSharedCalendars(shared.map(transformCalendarData));
    } catch (error) {
      handleApiError(error, '공유 캘린더 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // === 기존 호환성 유지 ===
  const shareCalendar = async (calendarId, shareData) => {
    // visibility를 PUBLIC으로 변경하는 것으로 구현
    return updateCalendar(calendarId, { 
      ...shareData, 
      visibility: 'PUBLIC' 
    });
  };

  const contextValue = {
    // 상태
    calendars,
    currentCalendar,
    setCurrentCalendar: switchCalendar, // 헬퍼 함수로 교체
    sharedCalendars,
    calendarMembers,
    loading,
    
    // 캘린더 CRUD
    createCalendar,
    updateCalendar,
    deleteCalendar,
    getCalendarById,
    
    // 멤버 관리
    inviteCalendarMembers,
    fetchCalendarMembers,
    respondToCalendarInvite,
    removeCalendarMember,
    
    // 유틸리티
    refreshCalendars: fetchCalendars,
    fetchSharedCalendars,
    getCalendarScheduleCount,
    switchCalendar,
    
    // 기존 호환성
    shareCalendar
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