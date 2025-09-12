import React, { useState } from 'react';
import { 
  Calendar, Plus, Users, BarChart3, Clock, MapPin, Edit, Bell,
  FileText 
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useCalendars } from '../../contexts/CalendarContext';
import { useSchedules } from '../../contexts/ScheduleContext';
import { useFriends } from '../../contexts/FriendContext';
import { useNotifications } from '../../contexts/NotificationContext';
import { useNavigate } from 'react-router-dom';
import { formatDate, formatTime, isToday, addDays, getMonthDays } from '../../utils/dateUtils';

export const Dashboard = () => {
  const { user } = useAuth();
  const { calendars } = useCalendars();
  const { schedules } = useSchedules();
  const { acceptedFriendships } = useFriends();
  const { notifications, unreadCount } = useNotifications();
  const navigate = useNavigate();
  
  const [currentDate, setCurrentDate] = useState(new Date());
  const [selectedDate, setSelectedDate] = useState(null);
  const [showScheduleDetail, setShowScheduleDetail] = useState(null);

  const months = [
    '1월', '2월', '3월', '4월', '5월', '6월',
    '7월', '8월', '9월', '10월', '11월', '12월'
  ];

  const weekdays = ['일', '월', '화', '수', '목', '금', '토'];

  const goToPreviousMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
  };

  const goToNextMonth = () => {
    setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
  };

  const getDaysInMonth = () => {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const days = getMonthDays(year, month);
    
    return days.map(date => {
      const isCurrentMonth = date.getMonth() === month;
      const dateKey = date.toISOString().split('T')[0];
      
      const daySchedules = schedules.filter(schedule => {
        const scheduleDate = new Date(schedule.startDateTime);
        return scheduleDate.toDateString() === date.toDateString();
      });
      
      return {
        date: date,
        day: date.getDate(),
        isCurrentMonth,
        isToday: isToday(date),
        dateKey,
        schedules: daySchedules
      };
    });
  };

  const days = getDaysInMonth();

  const todaySchedules = schedules.filter(schedule => 
    isToday(schedule.startDateTime)
  );

  const upcomingSchedules = schedules
    .filter(schedule => {
      const scheduleDate = new Date(schedule.startDateTime);
      return scheduleDate > new Date();
    })
    .sort((a, b) => new Date(a.startDateTime) - new Date(b.startDateTime))
    .slice(0, 5);

  const pageStyle = {
    padding: 0,
    fontFamily: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif'
  };

  const cardStyle = {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb'
  };

  return (
    <div style={pageStyle}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>
          안녕하세요, {user?.name}님!
        </h1>
        <p style={{ color: '#6b7280', marginBottom: 0 }}>
          오늘도 좋은 하루 되세요 ✨
        </p>
      </div>

      {/* 통계 카드 */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', 
        gap: '1rem', 
        marginBottom: '2rem' 
      }}>
        <div style={{
          ...cardStyle,
          background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
          color: 'white',
          padding: '1rem'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <p style={{ fontSize: '0.75rem', opacity: 0.9, marginBottom: '0.25rem' }}>오늘의 일정</p>
              <p style={{ fontSize: '1.5rem', fontWeight: 'bold', margin: 0 }}>
                {todaySchedules.length}
              </p>
            </div>
            <Calendar style={{ width: '2rem', height: '2rem', opacity: 0.8 }} />
          </div>
        </div>

        <div style={{
          ...cardStyle,
          background: 'linear-gradient(135deg, #10b981 0%, #047857 100%)',
          color: 'white',
          padding: '1rem'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <p style={{ fontSize: '0.75rem', opacity: 0.9, marginBottom: '0.25rem' }}>친구 수</p>
              <p style={{ fontSize: '1.5rem', fontWeight: 'bold', margin: 0 }}>
                {acceptedFriendships.length}
              </p>
            </div>
            <Users style={{ width: '2rem', height: '2rem', opacity: 0.8 }} />
          </div>
        </div>

        <div style={{
          ...cardStyle,
          background: 'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)',
          color: 'white',
          padding: '1rem'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <p style={{ fontSize: '0.75rem', opacity: 0.9, marginBottom: '0.25rem' }}>캘린더 수</p>
              <p style={{ fontSize: '1.5rem', fontWeight: 'bold', margin: 0 }}>
                {calendars.length}
              </p>
            </div>
            <FileText style={{ width: '2rem', height: '2rem', opacity: 0.8 }} />
          </div>
        </div>

        <div style={{
          ...cardStyle,
          background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
          color: 'white',
          padding: '1rem'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <p style={{ fontSize: '0.75rem', opacity: 0.9, marginBottom: '0.25rem' }}>읽지 않은 알림</p>
              <p style={{ fontSize: '1.5rem', fontWeight: 'bold', margin: 0 }}>
                {unreadCount}
              </p>
            </div>
            <Bell style={{ width: '2rem', height: '2rem', opacity: 0.8 }} />
          </div>
        </div>
      </div>

      {/* 메인 콘텐츠 영역 */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
        {/* 캘린더 섹션 */}
        <div style={cardStyle}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
            <h2 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>
              {currentDate.getFullYear()}년 {months[currentDate.getMonth()]}
            </h2>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button onClick={goToPreviousMonth} style={{
                padding: '0.5rem', borderRadius: '0.5rem', border: '1px solid #d1d5db',
                backgroundColor: 'white', cursor: 'pointer'
              }}>←</button>
              <button onClick={goToNextMonth} style={{
                padding: '0.5rem', borderRadius: '0.5rem', border: '1px solid #d1d5db',
                backgroundColor: 'white', cursor: 'pointer'
              }}>→</button>
              <button
                onClick={() => navigate('/schedules')}
                style={{
                  backgroundColor: '#2563eb', color: 'white', padding: '0.5rem 1rem',
                  borderRadius: '0.5rem', border: 'none', cursor: 'pointer',
                  display: 'flex', alignItems: 'center', gap: '0.5rem'
                }}
              >
                <Plus style={{ width: '1rem', height: '1rem' }} />
                일정 추가
              </button>
            </div>
          </div>

          {/* 캘린더 그리드 */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', marginBottom: '0.5rem' }}>
            {weekdays.map(day => (
              <div key={day} style={{ 
                padding: '0.75rem', textAlign: 'center', fontWeight: '600', 
                color: '#6b7280', borderBottom: '2px solid #e5e7eb'
              }}>
                {day}
              </div>
            ))}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '1px', backgroundColor: '#e5e7eb' }}>
            {days.map((day, index) => (
              <div
                key={index}
                onClick={() => setSelectedDate(day)}
                style={{
                  minHeight: '120px', padding: '0.5rem', backgroundColor: 'white',
                  cursor: 'pointer', opacity: day.isCurrentMonth ? 1 : 0.4,
                  border: selectedDate?.dateKey === day.dateKey ? '2px solid #3b82f6' : 'none'
                }}
              >
                <div style={{ 
                  fontWeight: day.isToday ? 'bold' : 'normal',
                  color: day.isToday ? '#3b82f6' : '#1f2937',
                  marginBottom: '0.25rem', fontSize: '0.875rem'
                }}>
                  {day.day}
                </div>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1px' }}>
                  {day.schedules.slice(0, 3).map((schedule) => (
                    <div
                      key={schedule.id}
                      onClick={(e) => {
                        e.stopPropagation();
                        setShowScheduleDetail(schedule);
                      }}
                      style={{
                        fontSize: '0.625rem', backgroundColor: schedule.color || '#3b82f6',
                        color: 'white', padding: '1px 4px', borderRadius: '2px',
                        cursor: 'pointer', overflow: 'hidden', textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap', maxWidth: '100%'
                      }}
                      title={schedule.title}
                    >
                      {schedule.title}
                    </div>
                  ))}
                  {day.schedules.length > 3 && (
                    <div style={{ 
                      fontSize: '0.625rem', color: '#6b7280',
                      textAlign: 'center', marginTop: '1px'
                    }}>
                      +{day.schedules.length - 3}
                    </div>
                  )}
                </div>

                {day.isToday && (
                  <div style={{
                    position: 'absolute', top: '4px', right: '4px',
                    width: '8px', height: '8px', backgroundColor: '#3b82f6',
                    borderRadius: '50%'
                  }} />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* 사이드 패널 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* 오늘의 일정 */}
          <div style={cardStyle}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: '600', color: '#1f2937', marginBottom: '1rem' }}>
              오늘의 일정
            </h3>
            {todaySchedules.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {todaySchedules.map(schedule => (
                  <div
                    key={schedule.id}
                    onClick={() => setShowScheduleDetail(schedule)}
                    style={{
                      padding: '0.75rem', backgroundColor: '#f8fafc',
                      borderRadius: '0.5rem', cursor: 'pointer',
                      borderLeft: `4px solid ${schedule.color || '#3b82f6'}`
                    }}
                  >
                    <h4 style={{ fontWeight: '600', color: '#1f2937', fontSize: '0.875rem', margin: 0 }}>
                      {schedule.title}
                    </h4>
                    <p style={{ fontSize: '0.75rem', color: '#6b7280', margin: '0.25rem 0 0 0' }}>
                      {formatTime(schedule.startDateTime)}
                      {schedule.location && ` • ${schedule.location}`}
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ color: '#6b7280', textAlign: 'center', padding: '2rem', margin: 0 }}>
                오늘 일정이 없습니다
              </p>
            )}
          </div>

          {/* 다가오는 일정 */}
          <div style={cardStyle}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: '600', color: '#1f2937', marginBottom: '1rem' }}>
              다가오는 일정
            </h3>
            {upcomingSchedules.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {upcomingSchedules.map(schedule => (
                  <div
                    key={schedule.id}
                    onClick={() => setShowScheduleDetail(schedule)}
                    style={{
                      padding: '0.75rem', backgroundColor: '#f8fafc',
                      borderRadius: '0.5rem', cursor: 'pointer'
                    }}
                  >
                    <h4 style={{ fontWeight: '600', color: '#1f2937', fontSize: '0.875rem', margin: 0 }}>
                      {schedule.title}
                    </h4>
                    <p style={{ fontSize: '0.75rem', color: '#6b7280', margin: '0.25rem 0 0 0' }}>
                      {formatDate(schedule.startDateTime)}
                      {schedule.location && ` • ${schedule.location}`}
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ color: '#6b7280', textAlign: 'center', padding: '2rem', margin: 0 }}>
                예정된 일정이 없습니다
              </p>
            )}
          </div>
        </div>
      </div>

      {/* 일정 상세 모달 */}
      {showScheduleDetail && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)', display: 'flex',
          alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}
        onClick={() => setShowScheduleDetail(null)}>
          <div style={{
            backgroundColor: 'white', padding: '2rem', borderRadius: '1rem',
            maxWidth: '500px', width: '90%', maxHeight: '80vh', overflow: 'auto'
          }}
          onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>
                {showScheduleDetail.title}
              </h3>
              <button
                onClick={() => setShowScheduleDetail(null)}
                style={{
                  padding: '0.5rem', borderRadius: '0.5rem', border: 'none',
                  backgroundColor: '#f3f4f6', cursor: 'pointer'
                }}
              >
                ✕
              </button>
            </div>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <h4 style={{ fontSize: '0.875rem', fontWeight: '600', color: '#6b7280', margin: '0 0 0.25rem 0' }}>
                  시간
                </h4>
                <p style={{ margin: 0, color: '#1f2937' }}>
                  {formatDate(showScheduleDetail.startDateTime)} {formatTime(showScheduleDetail.startDateTime)} - {formatTime(showScheduleDetail.endDateTime)}
                </p>
              </div>
              
              {showScheduleDetail.location && (
                <div>
                  <h4 style={{ fontSize: '0.875rem', fontWeight: '600', color: '#6b7280', margin: '0 0 0.25rem 0' }}>
                    장소
                  </h4>
                  <p style={{ margin: 0, color: '#1f2937' }}>
                    {showScheduleDetail.location}
                  </p>
                </div>
              )}
              
              {showScheduleDetail.description && (
                <div>
                  <h4 style={{ fontSize: '0.875rem', fontWeight: '600', color: '#6b7280', margin: '0 0 0.25rem 0' }}>
                    설명
                  </h4>
                  <p style={{ margin: 0, color: '#1f2937' }}>
                    {showScheduleDetail.description}
                  </p>
                </div>
              )}
              
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
                <button
                  onClick={() => {
                    setShowScheduleDetail(null);
                    navigate('/schedules');
                  }}
                  style={{
                    backgroundColor: '#2563eb', color: 'white', padding: '0.75rem 1rem',
                    border: 'none', borderRadius: '0.5rem', cursor: 'pointer',
                    display: 'flex', alignItems: 'center', gap: '0.5rem'
                  }}
                >
                  <Edit style={{ width: '1rem', height: '1rem' }} />
                  수정
                </button>
                <button
                  onClick={() => setShowScheduleDetail(null)}
                  style={{
                    padding: '0.75rem 1rem', border: '1px solid #d1d5db',
                    borderRadius: '0.5rem', backgroundColor: 'white',
                    color: '#374151', cursor: 'pointer'
                  }}
                >
                  닫기
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};