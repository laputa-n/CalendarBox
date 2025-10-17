import React from 'react';
import { Bell, CheckCircle, Trash2, Users, Calendar as CalendarIcon } from 'lucide-react';
import { useNotifications } from '../../contexts/NotificationContext';
import { formatDate, formatTime } from '../../utils/dateUtils';
import { NOTIFICATION_TYPES } from '../../utils/constants';
import { LoadingSpinner } from '../common/LoadingSpinner';

export const NotificationsPage = () => {
  const { 
    notifications, 
    markAsRead, 
    markAllAsRead, 
    deleteNotification,
    unreadCount, 
    loading 
  } = useNotifications();

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
    border: '1px solid #e5e7eb'
  };

  const buttonStyle = {
    backgroundColor: '#2563eb',
    color: 'white',
    padding: '0.75rem 1rem',
    borderRadius: '0.5rem',
    border: 'none',
    cursor: 'pointer',
    fontSize: '0.875rem',
    fontWeight: '500'
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>
            알림
          </h1>
          <p style={{ color: '#6b7280' }}>
            읽지 않은 알림이 {unreadCount}개 있습니다
          </p>
        </div>
        {unreadCount > 0 && (
          <button 
            onClick={markAllAsRead}
            disabled={loading}
            style={buttonStyle}
          >
            <CheckCircle style={{ width: '1rem', height: '1rem', marginRight: '0.5rem' }} />
            모두 읽음 처리
          </button>
        )}
      </div>

      <div style={cardStyle}>
        {loading ? (
          <LoadingSpinner text="알림을 불러오는 중..." />
        ) : notifications.length > 0 ? (
          notifications.map((notification, index) => (
            <div key={notification.id} style={{
              padding: '1.5rem',
              borderBottom: index < notifications.length - 1 ? '1px solid #e5e7eb' : 'none',
              backgroundColor: notification.isRead ? 'white' : '#f0f9ff',
              cursor: 'pointer',
              borderRadius: '0.5rem',
              margin: index > 0 ? '0.5rem 0 0 0' : 0
            }}
            onClick={() => !notification.isRead && markAsRead(notification.id)}>
              <div style={{ display: 'flex', alignItems: 'start', gap: '1rem' }}>
                <div style={{
                  width: '2.5rem',
                  height: '2.5rem',
                  backgroundColor: notification.isRead ? '#f3f4f6' : `${getNotificationColor(notification.type, notification.isRead)}20`,
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0
                }}>
                  <div style={{ color: getNotificationColor(notification.type, notification.isRead) }}>
                    {getNotificationIcon(notification.type)}
                  </div>
                </div>
                
                <div style={{ flex: 1 }}>
                  <p style={{ 
                    fontSize: '0.875rem', 
                    color: notification.isRead ? '#6b7280' : '#1f2937',
                    fontWeight: notification.isRead ? 'normal' : '500',
                    margin: 0,
                    marginBottom: '0.5rem',
                    lineHeight: '1.5'
                  }}>
                    {notification.message}
                  </p>
                  
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <p style={{ fontSize: '0.75rem', color: '#9ca3af', margin: 0 }}>
                      {formatDate(notification.createdAt)} {formatTime(notification.createdAt)}
                    </p>
                    
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      {!notification.isRead && (
                        <div style={{
                          width: '0.5rem',
                          height: '0.5rem',
                          backgroundColor: '#2563eb',
                          borderRadius: '50%'
                        }} />
                      )}
                      
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          if (window.confirm('이 알림을 삭제하시겠습니까?')) {
                            deleteNotification(notification.id);
                          }
                        }}
                        style={{
                          padding: '0.25rem',
                          color: '#6b7280',
                          backgroundColor: 'transparent',
                          border: 'none',
                          borderRadius: '0.25rem',
                          cursor: 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center'
                        }}
                        title="알림 삭제"
                      >
                        <Trash2 style={{ width: '0.875rem', height: '0.875rem' }} />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))
        ) : (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#6b7280' }}>
            <Bell style={{ width: '4rem', height: '4rem', color: '#d1d5db', margin: '0 auto 1rem auto' }} />
            <p style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>알림이 없습니다.</p>
            <p style={{ color: '#9ca3af' }}>새로운 알림이 있으면 여기에 표시됩니다.</p>
          </div>
        )}
      </div>
    </div>
  );
};
