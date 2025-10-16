import React, { createContext, useContext, useState, useEffect } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useError } from './ErrorContext';

const NotificationContext = createContext();

export const NotificationProvider = ({ children }) => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    page: 1,
    limit: 20,
    total: 0,
    hasMore: false
  });
  
  const { user, isAuthenticated } = useAuth();
  const { showError } = useError();

  useEffect(() => {
    if (user) {
      fetchNotifications();
    }
  }, [user]);

  const fetchNotifications = async (page = 1, append = false) => {
    if (!user) return;
    
    try {
      setLoading(true);
      const response = await ApiService.getNotifications({
        page,
        limit: pagination.limit
      });
      
      const notificationData = response.data || response.notifications || response;
      const newNotifications = Array.isArray(notificationData) ? notificationData : [];
      
      setNotifications(prev => 
        append ? [...prev, ...newNotifications] : newNotifications
      );
      
      // 페이지네이션 정보 업데이트
      if (response.pagination) {
        setPagination(response.pagination);
      }
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
      showError('알림 조회에 실패했습니다.');
      
      // 개발 시 목 데이터
      if (!append) {
        setNotifications([
          {
            id: 1,
            type: 'friend_request',
            message: '김친구님이 친구 요청을 보냈습니다.',
            isRead: false,
            createdAt: new Date().toISOString(),
            data: {
              friendshipId: 1
            }
          },
          {
            id: 2,
            type: 'schedule_invite',
            message: '새로운 일정에 초대되었습니다.',
            isRead: false,
            createdAt: new Date(Date.now() - 3600000).toISOString(),
            data: {
              scheduleId: 1,
              inviterName: '박동료'
            }
          }
        ]);
      }
    } finally {
      setLoading(false);
    }
  };

  const markAsRead = async (notificationId) => {
    try {
      await ApiService.markNotificationAsRead(notificationId);
      setNotifications(prev => prev.map(notif => 
        notif.id === notificationId 
          ? { ...notif, isRead: true }
          : notif
      ));
    } catch (error) {
      showError(error.message || '알림 읽음 처리에 실패했습니다.');
      throw error;
    }
  };

  const markAllAsRead = async () => {
    try {
      setLoading(true);
      await ApiService.markAllNotificationsAsRead();
      setNotifications(prev => prev.map(notif => ({ ...notif, isRead: true })));
    } catch (error) {
      showError(error.message || '전체 알림 읽음 처리에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const deleteNotification = async (notificationId) => {
    try {
      await ApiService.deleteNotification(notificationId);
      setNotifications(prev => prev.filter(notif => notif.id !== notificationId));
    } catch (error) {
      showError(error.message || '알림 삭제에 실패했습니다.');
      throw error;
    }
  };

  const loadMoreNotifications = async () => {
    if (pagination.hasMore && !loading) {
      await fetchNotifications(pagination.page + 1, true);
    }
  };

  // 읽지 않은 알림 개수
  const unreadCount = notifications.filter(n => !n.isRead).length;

  // 알림 타입별 필터링
  const getNotificationsByType = (type) => {
    return notifications.filter(n => n.type === type);
  };

  const contextValue = {
    notifications,
    loading,
    pagination,
    unreadCount,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    loadMoreNotifications,
    getNotificationsByType,
    refreshNotifications: () => fetchNotifications(1, false)
  };

  return (
    <NotificationContext.Provider value={contextValue}>
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
};