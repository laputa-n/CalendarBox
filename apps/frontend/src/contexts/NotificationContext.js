import React, { createContext, useContext, useState, useEffect } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useError } from './ErrorContext';


  const NotificationContext = createContext();


export const NotificationProvider = ({ children }) => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    last: true
  });

  const { user } = useAuth();
  const { showError } = useError();

  useEffect(() => {
    if (user) fetchNotifications();
  }, [user]);

  // ===== 알림 목록 조회 =====
  const fetchNotifications = async (page = 0, append = false) => {
    try {
      setLoading(true);

      const response = await ApiService.getNotifications({
        page,
        size: pagination.size
      });

      const data = response.data;
      if (!data) return;

      const mapped = data.notifications.map(n => ({
        id: n.id,
        type: n.type,
        payloadJson: n.payloadJson,
        createdAt: n.createdAt,
        readAt: n.readAt,
        actor: n.actor,
        isRead: !!n.readAt
      }));

      setNotifications(prev =>
        append ? [...prev, ...mapped] : mapped
      );

      setPagination({
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
        last: data.last
      });
    } catch (e) {
      console.error(e);
      showError('알림 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

// ===== 캘린더 초대 응답 =====
const respondCalendarInvite = async (notificationId, action) => {
  try {
    await ApiService.respondCalendarInvite(notificationId, action);

    // ✅ 알림 제거
    setNotifications(prev =>
      prev.filter(n => n.id !== notificationId)
    );
  } catch (e) {
    showError('캘린더 초대 응답에 실패했습니다.');
  }
};



  // ===== 알림 읽음 =====
  const markAsRead = async (notificationId) => {
    try {
      await ApiService.markNotificationAsRead(notificationId);

      setNotifications(prev =>
        prev.map(n =>
          n.id === notificationId
            ? { ...n, isRead: true, readAt: new Date().toISOString() }
            : n
        )
      );
    } catch (e) {
      showError('알림 읽음 처리에 실패했습니다.');
    }
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        loading,
        pagination,
        unreadCount,
        markAsRead,
        respondCalendarInvite, 
        refreshNotifications: () => fetchNotifications(0, false)

      }}
    >
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
