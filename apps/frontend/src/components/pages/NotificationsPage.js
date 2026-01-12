import React, { useEffect } from "react";
import { Bell, Users, Calendar as CalendarIcon } from 'lucide-react';
import { useNotifications } from '../../contexts/NotificationContext';
import { formatDate, formatTime } from '../../utils/dateUtils';
import { NOTIFICATION_TYPES } from '../../utils/constants';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { useNavigate } from "react-router-dom";

export const NotificationsPage = () => {
 const {
  refreshNotifications,
  notifications,
  markAsRead,
  unreadCount,
  loading,
  respondCalendarInvite,
} = useNotifications();

useEffect(() => {
  refreshNotifications();
}, []);

  const navigate = useNavigate();
  const getNotificationIcon = (type) => {
    switch (type) {
      case NOTIFICATION_TYPES.FRIEND_REQUEST:
        return <Users style={{ width: '1.25rem', height: '1.25rem' }} />;
      case NOTIFICATION_TYPES.SCHEDULE_INVITE:
      case NOTIFICATION_TYPES.SCHEDULE_REMINDER:
      case NOTIFICATION_TYPES.SCHEDULE_UPDATED:
        return <CalendarIcon style={{ width: '1.25rem', height: '1.25rem' }} />;
      default:
        return <Bell style={{ width: '1.25rem', height: '1.25rem' }} />;
    }
  };

  const getNotificationMessage = (notification) => {
    switch (notification.type) {
      case 'RECEIVED_FRIEND_REQUEST':
        return `${notification.actor?.name}님이 친구 요청을 보냈습니다.`;
      case 'INVITED_TO_CALENDAR':
        return `${notification.actor?.name}님이 캘린더에 초대했습니다.`;
      case 'INVITED_TO_SCHEDULE':
        return `${notification.actor?.name}님이 일정에 초대했습니다.`;
      case 'SYSTEM':
        return '시스템 알림이 도착했습니다.';
      default:
        return '새로운 알림이 도착했습니다.';
    }
  };

  const getNotificationColor = (type, isRead) => {
    if (isRead) return '#6b7280';

    switch (type) {
      case NOTIFICATION_TYPES.FRIEND_REQUEST:
        return '#10b981';
      case NOTIFICATION_TYPES.SCHEDULE_INVITE:
        return '#3b82f6';
      case NOTIFICATION_TYPES.SCHEDULE_REMINDER:
        return '#f59e0b';
      case NOTIFICATION_TYPES.SCHEDULE_UPDATED:
        return '#8b5cf6';
      default:
        return '#2563eb';
    }
  };

  const cardStyle = {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb',
  };

  const buttonStyle = {
    padding: '0.5rem 0.75rem',
    borderRadius: '0.5rem',
    border: 'none',
    cursor: 'pointer',
    fontSize: '0.75rem',
    fontWeight: '600',
  };

  return (
    <div>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold' }}>알림</h1>
        <p style={{ color: '#6b7280' }}>
          읽지 않은 알림이 {unreadCount}개 있습니다
        </p>
      </div>

      <div style={cardStyle}>
        {loading ? (
          <LoadingSpinner text="알림을 불러오는 중..." />
        ) : notifications.length > 0 ? (
         notifications.map((notification, index) => (
  <div
    key={notification.id}
    style={{
      padding: '1.25rem',
      borderBottom:
        index < notifications.length - 1
          ? '1px solid #e5e7eb'
          : 'none',
      backgroundColor: notification.isRead ? 'white' : '#f0f9ff',
      borderRadius: '0.5rem',
      marginTop: index > 0 ? '0.5rem' : 0,
      cursor: 'pointer',
    }}
  onClick={() => {
  if (!notification.isRead) {
    markAsRead(notification.id);
  }
}}
  >
    <div style={{ display: 'flex', gap: '1rem' }}>
      {/* 아이콘 */}
      <div
        style={{
          width: '2.5rem',
          height: '2.5rem',
          borderRadius: '50%',
          backgroundColor: `${getNotificationColor(
            notification.type,
            notification.isRead
          )}20`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <div
          style={{
            color: getNotificationColor(
              notification.type,
              notification.isRead
            ),
          }}
        >
          {getNotificationIcon(notification.type)}
        </div>
      </div>

      {/* 내용 */}
      <div style={{ flex: 1 }}>
        <p
          style={{
            fontSize: '0.875rem',
            fontWeight: notification.isRead ? 400 : 600,
            marginBottom: '0.25rem',
          }}
        >
          {getNotificationMessage(notification)}
        </p>

        <p style={{ fontSize: '0.75rem', color: '#9ca3af' }}>
          {formatDate(notification.createdAt)}{' '}
          {formatTime(notification.createdAt)}  
        </p>
          {notification.type === 'INVITED_TO_CALENDAR' && !notification.isRead && (
  <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem' }}>
    <button
      style={{ ...buttonStyle, background: '#2563eb', color: 'white' }}
      onClick={async (e) => {
        e.stopPropagation();

        await respondCalendarInvite(notification.id, 'ACCEPT');

        // ✅ 수락 성공 후 캘린더로 이동
        const raw = notification.payloadJson || '';
        const match = raw.match(/calendarId=(\d+)/);
        if (match) {
          navigate(`/calendar/${match[1]}`);
        }
      }}
    >
      수락
    </button>

    <button
      style={{ ...buttonStyle, background: '#e5e7eb', color: '#374151' }}
      onClick={(e) => {
        e.stopPropagation();
        respondCalendarInvite(notification.id, 'REJECT');
      }}
    >
      거절
    </button>
  </div>
)}

      </div>
    </div>
  </div>
))

        ) : (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#6b7280' }}>
            <Bell
              style={{
                width: '4rem',
                height: '4rem',
                margin: '0 auto 1rem',
                color: '#d1d5db',
              }}
            />
            <p>알림이 없습니다.</p>
          </div>
        )}
      </div>
    </div>
  );
};
