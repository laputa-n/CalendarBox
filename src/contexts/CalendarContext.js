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
  
  const { user , isAuthenticated} = useAuth();
  const { showError } = useError();

useEffect(() => {
  if (isAuthenticated && user) { // 둘 다 체크
    fetchCalendars();
  }
}, [isAuthenticated, user]); // 의존성에 둘 다 추가

const fetchCalendars = async () => {
  if (!isAuthenticated || !user) return;
    
    try {
      setLoading(true);
      const response = await ApiService.getCalendars();
      const calendarData = response.data || response;
      
      setCalendars(calendarData);
      
      // 현재 캘린더가 없으면 첫 번째 캘린더를 선택
      if (calendarData.length > 0 && !currentCalendar) {
        setCurrentCalendar(calendarData[0]);
      }
    } catch (error) {
      console.error('Failed to fetch calendars:', error);
      showError('캘린더 조회에 실패했습니다.');
      
      // 개발 시 목 데이터 사용
      const mockCalendars = [
        {
          id: 1,
          name: '개인 캘린더',
          description: '나의 개인 일정',
          color: '#3b82f6',
          isShared: false,
          createdAt: new Date().toISOString()
        }
      ];
      setCalendars(mockCalendars);
      setCurrentCalendar(mockCalendars[0]);
    } finally {
      setLoading(false);
    }
  };

  const createCalendar = async (calendarData) => {
    try {
      setLoading(true);
      const response = await ApiService.createCalendar(calendarData);
      await fetchCalendars();
      return response;
    } catch (error) {
      showError(error.message || '캘린더 생성에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const updateCalendar = async (calendarId, calendarData) => {
    try {
      setLoading(true);
      const response = await ApiService.updateCalendar(calendarId, calendarData);
      await fetchCalendars();
      return response;
    } catch (error) {
      showError(error.message || '캘린더 수정에 실패했습니다.');
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
      if (currentCalendar?.id === calendarId) {
        setCurrentCalendar(null);
      }
      
      await fetchCalendars();
    } catch (error) {
      showError(error.message || '캘린더 삭제에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const shareCalendar = async (calendarId, shareData) => {
    try {
      setLoading(true);
      const response = await ApiService.shareCalendar(calendarId, shareData);
      await fetchCalendars();
      return response;
    } catch (error) {
      showError(error.message || '캘린더 공유에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const contextValue = {
    calendars,
    currentCalendar,
    setCurrentCalendar,
    sharedCalendars,
    loading,
    createCalendar,
    updateCalendar,
    deleteCalendar,
    shareCalendar,
    refreshCalendars: fetchCalendars
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