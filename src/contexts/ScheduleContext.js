// src/contexts/ScheduleContext.js

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
  const [participants, setParticipants] = useState([]);
  const [places, setPlaces] = useState([]);
  
  const { currentCalendar } = useCalendars();
  const { user } = useAuth();
  const { showError } = useError();

  useEffect(() => {
    if (currentCalendar && user) {
      fetchSchedules();
    }
  }, [currentCalendar, user]);

  // 백엔드 데이터를 프론트엔드 형식으로 변환
  const transformScheduleData = (backendSchedule) => {
    return {
      // API 명세 필드들
      id: backendSchedule.scheduleId,
      scheduleId: backendSchedule.scheduleId,
      calendarId: backendSchedule.calendarId,
      title: backendSchedule.scheduleTitle || backendSchedule.title,
      memo: backendSchedule.memo,
      theme: backendSchedule.theme,
      startAt: backendSchedule.startAt,
      endAt: backendSchedule.endAt,
      createdBy: backendSchedule.createdBy,
      updatedBy: backendSchedule.updatedBy,
      createdAt: backendSchedule.createdAt,
      updatedAt: backendSchedule.updatedAt,
      
      // 상세 조회 시 추가 필드들
      summary: backendSchedule.summary,
      
      // 기존 UI 호환성을 위한 필드들
      startDateTime: backendSchedule.startAt,
      endDateTime: backendSchedule.endAt,
      description: backendSchedule.memo,
      isAllDay: false, // 추후 API에서 지원시 수정
      location: '', // 장소는 별도 API로 관리
      color: backendSchedule.theme?.toLowerCase() || '#3b82f6',
      visibility: 'public',
      status: 'confirmed',
      participants: []
    };
  };

  // 일정 목록 조회 (현재 캘린더 기준)
  const fetchSchedules = async (params = {}) => {
    if (!currentCalendar || !user) return;
    
    try {
      setLoading(true);
      // API 명세: GET /schedules (전체) 또는 GET /calendars/{id}/schedules (특정 캘린더)
      const response = await ApiService.getCalendarSchedules(currentCalendar.id, params);
      
      // API 명세: response.scheduleList 배열
      const scheduleList = response.scheduleList || [];
      const transformedSchedules = scheduleList.map(transformScheduleData);
      
      setSchedules(transformedSchedules);
    } catch (error) {
      console.error('Failed to fetch schedules:', error);
      showError('일정 조회에 실패했습니다.');
      
      // 개발 시 목 데이터 (API 명세 형식으로 수정)
      const mockSchedules = [
        {
          scheduleId: 1,
          calendarId: currentCalendar.id,
          calendarType: 'PERSONAL',
          calendarName: currentCalendar.name,
          scheduleTitle: '팀 회의',
          startAt: new Date().toISOString(),
          endAt: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
          memo: '프로젝트 진행상황 점검',
          theme: 'BLUE',
          createdBy: user.id,
          createdAt: new Date().toISOString()
        }
      ];
      setSchedules(mockSchedules.map(transformScheduleData));
    } finally {
      setLoading(false);
    }
  };

  // 전체 일정 조회 (모든 캘린더)
  const fetchAllSchedules = async (params = {}) => {
    if (!user) return;
    
    try {
      setLoading(true);
      // API 명세: GET /schedules
      const response = await ApiService.getSchedules(params);
      
      const scheduleList = response.scheduleList || [];
      const transformedSchedules = scheduleList.map(transformScheduleData);
      
      setSchedules(transformedSchedules);
    } catch (error) {
      console.error('Failed to fetch all schedules:', error);
      showError('전체 일정 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 일정 생성 (API 명세: POST /calendars/{calendarId}/schedules)
  const createSchedule = async (scheduleData) => {
    if (!currentCalendar) {
      showError('캘린더를 선택해주세요.');
      return;
    }

    try {
      setLoading(true);
      
      // UI 데이터를 API 명세 형식으로 변환
      const apiData = {
        title: scheduleData.title,
        memo: scheduleData.memo || scheduleData.description || '',
        theme: scheduleData.theme || scheduleData.color?.toUpperCase() || 'BLUE',
        startAt: scheduleData.startAt || scheduleData.startDateTime,
        endAt: scheduleData.endAt || scheduleData.endDateTime
      };
      
      const response = await ApiService.createSchedule(currentCalendar.id, apiData);
      await fetchSchedules(); // 목록 새로고침
      return response;
    } catch (error) {
      showError(error.message || '일정 생성에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 일정 수정 (API 명세: PUT /schedules/{scheduleId})
  const updateSchedule = async (scheduleId, scheduleData) => {
    try {
      setLoading(true);
      
      // UI 데이터를 API 명세 형식으로 변환 (변경된 필드만)
      const apiData = {};
      if (scheduleData.title) apiData.title = scheduleData.title;
      if (scheduleData.memo !== undefined) apiData.memo = scheduleData.memo;
      if (scheduleData.theme) apiData.theme = scheduleData.theme;
      if (scheduleData.startAt || scheduleData.startDateTime) {
        apiData.startAt = scheduleData.startAt || scheduleData.startDateTime;
      }
      if (scheduleData.endAt || scheduleData.endDateTime) {
        apiData.endAt = scheduleData.endAt || scheduleData.endDateTime;
      }
      
      await ApiService.updateSchedule(scheduleId, apiData);
      await fetchSchedules();
    } catch (error) {
      showError(error.message || '일정 수정에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 일정 삭제 (API 명세: DELETE /schedules/{scheduleId})
  const deleteSchedule = async (scheduleId) => {
    try {
      setLoading(true);
      await ApiService.deleteSchedule(scheduleId);
      
      // 삭제된 일정이 현재 선택된 일정인 경우 처리
      if (currentSchedule?.scheduleId === scheduleId) {
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

  // 일정 검색 (API 명세: GET /schedules/search)
  const searchSchedules = async (query, filters = {}) => {
    try {
      setLoading(true);
      const response = await ApiService.searchSchedules(query, filters);
      
      const scheduleList = response.scheduleList || [];
      const transformedResults = scheduleList.map(transformScheduleData);
      
      setSearchResults(transformedResults);
      return transformedResults;
    } catch (error) {
      showError(error.message || '일정 검색에 실패했습니다.');
      return [];
    } finally {
      setLoading(false);
    }
  };

  // 일정 상세 조회 (API 명세: GET /schedules/{scheduleId})
  const getScheduleById = async (scheduleId) => {
    try {
      const response = await ApiService.getScheduleById(scheduleId);
      const transformedSchedule = transformScheduleData(response);
      setCurrentSchedule(transformedSchedule);
      return transformedSchedule;
    } catch (error) {
      showError(error.message || '일정 조회에 실패했습니다.');
      throw error;
    }
  };

  // 일정 복제 (API 명세: POST /calendars/{calendarId}/schedules - 복제)
  const duplicateSchedule = async (scheduleId, targetCalendarId = null) => {
    const calendarId = targetCalendarId || currentCalendar?.id;
    if (!calendarId) {
      showError('캘린더를 선택해주세요.');
      return;
    }

    try {
      setLoading(true);
      const response = await ApiService.duplicateSchedule(calendarId, scheduleId);
      await fetchSchedules();
      return response;
    } catch (error) {
      showError(error.message || '일정 복제에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // === 일정 참여자 관리 (API 명세 새로 추가) ===

  // 일정 참여자 추가 (API 명세: POST /schedules/{scheduleId}/participants)
  const addScheduleParticipant = async (scheduleId, participantData) => {
    try {
      setLoading(true);
      const response = await ApiService.addScheduleParticipant(scheduleId, participantData);
      await fetchScheduleParticipants(scheduleId); // 참여자 목록 새로고침
      return response;
    } catch (error) {
      showError(error.message || '참여자 추가에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 일정 참여자 목록 조회 (API 명세: GET /schedules/{scheduleId}/participants)
  const fetchScheduleParticipants = async (scheduleId) => {
    try {
      const response = await ApiService.getScheduleParticipants(scheduleId);
      const participantList = Array.isArray(response) ? response : [];
      setParticipants(participantList);
      return participantList;
    } catch (error) {
      console.error('Failed to fetch participants:', error);
      showError('참여자 목록 조회에 실패했습니다.');
      return [];
    }
  };

  // 일정 참여자 삭제 (API 명세: DELETE /schedules/{scheduleId}/participants/{participantId})
  const removeScheduleParticipant = async (scheduleId, participantId) => {
    try {
      setLoading(true);
      await ApiService.removeScheduleParticipant(scheduleId, participantId);
      await fetchScheduleParticipants(scheduleId);
    } catch (error) {
      showError(error.message || '참여자 제거에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 일정 초대 응답 (API 명세: PUT /schedules/{scheduleId}/participants/{participantId})
  const respondToScheduleInvite = async (scheduleId, participantId, action) => {
    try {
      setLoading(true);
      // action: 'ACCEPT' or 'REJECT'
      const response = await ApiService.respondToScheduleInvite(scheduleId, participantId, action);
      await fetchScheduleParticipants(scheduleId);
      return response;
    } catch (error) {
      showError(error.message || '초대 응답에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // === 일정 장소 관리 (API 명세 새로 추가) ===

  // 장소 검색 (API 명세: GET /places/search)
  const searchPlaces = async (query) => {
    try {
      const response = await ApiService.searchPlaces(query);
      return response.content || []; // API 명세: content 배열
    } catch (error) {
      showError(error.message || '장소 검색에 실패했습니다.');
      return [];
    }
  };

  // 일정 장소 추가 (API 명세: POST /schedules/{scheduleId}/places)
  const addSchedulePlace = async (scheduleId, placeData) => {
    try {
      setLoading(true);
      const response = await ApiService.addSchedulePlace(scheduleId, placeData);
      await fetchSchedulePlaces(scheduleId);
      return response;
    } catch (error) {
      showError(error.message || '장소 추가에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 일정 장소 목록 조회
  const fetchSchedulePlaces = async (scheduleId) => {
    try {
      const response = await ApiService.getSchedulePlaces(scheduleId);
      const placeList = Array.isArray(response) ? response : [];
      setPlaces(placeList);
      return placeList;
    } catch (error) {
      console.error('Failed to fetch places:', error);
      return [];
    }
  };

  // 일정 장소 순서 변경 (API 명세: PUT /schedules/{scheduleId}/places)
  const reorderSchedulePlaces = async (scheduleId, placesOrder) => {
    try {
      setLoading(true);
      await ApiService.reorderSchedulePlaces(scheduleId, placesOrder);
      await fetchSchedulePlaces(scheduleId);
    } catch (error) {
      showError(error.message || '장소 순서 변경에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 일정 장소 삭제 (API 명세: DELETE /schedules/{scheduleId}/places/{schedulePlaceId})
  const removeSchedulePlace = async (scheduleId, schedulePlaceId) => {
    try {
      setLoading(true);
      await ApiService.removeSchedulePlace(scheduleId, schedulePlaceId);
      await fetchSchedulePlaces(scheduleId);
    } catch (error) {
      showError(error.message || '장소 삭제에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // === 기존 호환성 유지 ===
  const inviteToSchedule = async (scheduleId, inviteData) => {
    // 새로운 API로 리다이렉트
    return addScheduleParticipant(scheduleId, inviteData);
  };

  const contextValue = {
    // 상태
    schedules,
    currentSchedule,
    setCurrentSchedule,
    searchResults,
    participants,
    places,
    loading,
    
    // 일정 CRUD
    createSchedule,
    updateSchedule,
    deleteSchedule,
    searchSchedules,
    getScheduleById,
    duplicateSchedule,
    
    // 참여자 관리
    addScheduleParticipant,
    fetchScheduleParticipants,
    removeScheduleParticipant,
    respondToScheduleInvite,
    
    // 장소 관리
    searchPlaces,
    addSchedulePlace,
    fetchSchedulePlaces,
    reorderSchedulePlaces,
    removeSchedulePlace,
    
    // 유틸리티
    refreshSchedules: fetchSchedules,
    fetchAllSchedules,
    
    // 기존 호환성
    inviteToSchedule
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