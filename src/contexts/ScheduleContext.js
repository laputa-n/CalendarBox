import React, { createContext, useContext, useState, useEffect } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useCalendars } from './CalendarContext';
import { useError } from './ErrorContext';

const ScheduleContext = createContext();

export const ScheduleProvider = ({ children }) => {
  const [schedules, setSchedules] = useState([]);
  const [currentSchedule, setCurrentSchedule] = useState(null);
  const [loading, setLoading] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  
  const { currentCalendar } = useCalendars();
  const { user } = useAuth();
  const { showError } = useError();

  useEffect(() => {
    if (currentCalendar && user ) {
      fetchSchedules();
    }
  }, [currentCalendar , user]);

  const fetchSchedules = async (params = {}) => {
    if (!currentCalendar || !user ) return;
    
    try {
      setLoading(true);
      const response = await ApiService.getSchedules(currentCalendar.id, params);
      setSchedules(response.data || response);
    } catch (error) {
      console.error('Failed to fetch schedules:', error);
      showError('일정 조회에 실패했습니다.');
      
      // 개발 시 목 데이터
      setSchedules([
        {
          id: 1,
          title: '팀 회의',
          description: '프로젝트 진행상황 점검',
          startDateTime: new Date().toISOString(),
          endDateTime: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
          isAllDay: false,
          location: '회의실 A',
          color: '#3b82f6',
          visibility: 'public',
          status: 'confirmed',
          createdAt: new Date().toISOString(),
          participants: []
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const createSchedule = async (scheduleData) => {
    if (!currentCalendar) {
      showError('캘린더를 선택해주세요.');
      return;
    }

    try {
      setLoading(true);
      const response = await ApiService.createSchedule(currentCalendar.id, scheduleData);
      await fetchSchedules();
      return response;
    } catch (error) {
      showError(error.message || '일정 생성에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const updateSchedule = async (scheduleId, scheduleData) => {
    try {
      setLoading(true);
      await ApiService.updateSchedule(scheduleId, scheduleData);
      await fetchSchedules();
    } catch (error) {
      showError(error.message || '일정 수정에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const deleteSchedule = async (scheduleId) => {
    try {
      setLoading(true);
      await ApiService.deleteSchedule(scheduleId);
      
      // 삭제된 일정이 현재 선택된 일정인 경우 처리
      if (currentSchedule?.id === scheduleId) {
        setCurrentSchedule(null);
      }
      
      await fetchSchedules();
    } catch (error) {
      showError(error.message || '일정 삭제에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const searchSchedules = async (query, filters = {}) => {
    try {
      setLoading(true);
      const response = await ApiService.searchSchedules(query, filters);
      const results = response.data || response;
      setSearchResults(results);
      return results;
    } catch (error) {
      showError(error.message || '일정 검색에 실패했습니다.');
      return [];
    } finally {
      setLoading(false);
    }
  };

  const getScheduleById = async (scheduleId) => {
    try {
      const response = await ApiService.getScheduleById(scheduleId);
      return response.data || response;
    } catch (error) {
      showError(error.message || '일정 조회에 실패했습니다.');
      throw error;
    }
  };

  const inviteToSchedule = async (scheduleId, inviteData) => {
    try {
      setLoading(true);
      const response = await ApiService.inviteToSchedule(scheduleId, inviteData);
      await fetchSchedules();
      return response;
    } catch (error) {
      showError(error.message || '초대 전송에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const contextValue = {
    schedules,
    currentSchedule,
    setCurrentSchedule,
    searchResults,
    loading,
    createSchedule,
    updateSchedule,
    deleteSchedule,
    searchSchedules,
    getScheduleById,
    inviteToSchedule,
    refreshSchedules: fetchSchedules
  };

  return (
    <ScheduleContext.Provider value={contextValue}>
      {children}
    </ScheduleContext.Provider>
  );
};

export const useSchedules = () => {
  const context = useContext(ScheduleContext);
  if (!context) {
    throw new Error('useSchedules must be used within a ScheduleProvider');
  }
  return context;
};