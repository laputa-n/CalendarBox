import React, { createContext, useContext, useEffect } from 'react';
import { realtimeService } from '../services/realtimeService';
import { useAuth } from './AuthContext';
import { useSchedules } from './ScheduleContext';
import { useNotifications } from './NotificationContext';
import { useFriends } from './FriendContext';

const RealtimeContext = createContext();

export const RealtimeProvider = ({ children }) => {
  const { user } = useAuth();
  const { refreshSchedules } = useSchedules();
  const { refreshNotifications } = useNotifications();
  const { refreshFriendships } = useFriends();

  useEffect(() => {
    if (!user) return;

    // WebSocket 연결
    realtimeService.connect(user.id);

    // 실시간 이벤트 리스너 등록
    const handleScheduleUpdate = (data) => {
      console.log('Schedule updated:', data);
      refreshSchedules();
    };

    const handleNotificationReceived = (data) => {
      console.log('Notification received:', data);
      refreshNotifications();
      
      // 브라우저 알림 표시
      if (Notification.permission === 'granted') {
        new Notification(data.title || '새 알림', {
          body: data.message,
          icon: '/favicon.ico'
        });
      }
    };

    const handleFriendshipUpdate = (data) => {
      console.log('Friendship updated:', data);
      refreshFriendships();
    };

    const handleCalendarShare = (data) => {
      console.log('Calendar shared:', data);
      // 캘린더 공유 처리
    };

    // 이벤트 리스너 등록
    realtimeService.on('schedule_created', handleScheduleUpdate);
    realtimeService.on('schedule_updated', handleScheduleUpdate);
    realtimeService.on('schedule_deleted', handleScheduleUpdate);
    realtimeService.on('notification_received', handleNotificationReceived);
    realtimeService.on('friendship_request', handleFriendshipUpdate);
    realtimeService.on('friendship_accepted', handleFriendshipUpdate);
    realtimeService.on('calendar_shared', handleCalendarShare);

    // 연결 상태 모니터링
    realtimeService.on('connected', () => {
      console.log('Real-time connection established');
    });

    realtimeService.on('disconnected', () => {
      console.log('Real-time connection lost');
    });

    // 정리
    return () => {
      realtimeService.off('schedule_created', handleScheduleUpdate);
      realtimeService.off('schedule_updated', handleScheduleUpdate);
      realtimeService.off('schedule_deleted', handleScheduleUpdate);
      realtimeService.off('notification_received', handleNotificationReceived);
      realtimeService.off('friendship_request', handleFriendshipUpdate);
      realtimeService.off('friendship_accepted', handleFriendshipUpdate);
      realtimeService.off('calendar_shared', handleCalendarShare);
      
      realtimeService.disconnect();
    };
  }, [user, refreshSchedules, refreshNotifications, refreshFriendships]);

  // 브라우저 알림 권한 요청
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }, []);

  return (
    <RealtimeContext.Provider value={{ realtimeService }}>
      {children}
    </RealtimeContext.Provider>
  );
};

export const useRealtime = () => {
  const context = useContext(RealtimeContext);
  if (!context) {
    throw new Error('useRealtime must be used within a RealtimeProvider');
  }
  return context;
};

// components/common/LazyImage.js
import React, { useState, useRef } from 'react';
import { useIntersectionObserver } from '../../hooks/usePerformance';

export const LazyImage = ({ 
  src, 
  alt, 
  placeholder = '/placeholder.jpg',
  className,
  style,
  ...props 
}) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [isInView, setIsInView] = useState(false);
  const [error, setError] = useState(false);

  const handleIntersection = (entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        setIsInView(true);
      }
    });
  };

  const imgRef = useIntersectionObserver(handleIntersection, {
    threshold: 0.1,
    rootMargin: '50px'
  });

  const handleLoad = () => {
    setIsLoaded(true);
  };

  const handleError = () => {
    setError(true);
  };

  return (
    <div ref={imgRef} style={{ position: 'relative', ...style }}>
      {!isInView ? (
        <div
          style={{
            width: '100%',
            height: '100%',
            backgroundColor: '#f3f4f6',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <div style={{ color: '#9ca3af', fontSize: '0.875rem' }}>
            Loading...
          </div>
        </div>
      ) : (
        <>
          <img
            src={error ? placeholder : src}
            alt={alt}
            className={className}
            onLoad={handleLoad}
            onError={handleError}
            style={{
              opacity: isLoaded ? 1 : 0,
              transition: 'opacity 0.3s ease-in-out',
              width: '100%',
              height: '100%',
              objectFit: 'cover'
            }}
            {...props}
          />
          
          {!isLoaded && (
            <div
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                backgroundColor: '#f3f4f6',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <div style={{ color: '#9ca3af', fontSize: '0.875rem' }}>
                Loading...
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};
