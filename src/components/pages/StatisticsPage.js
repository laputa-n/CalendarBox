import React, { useState, useEffect } from 'react';
import { BarChart3, Calendar as CalendarIcon, Users, TrendingUp, Activity } from 'lucide-react';
import { useCalendars } from '../../contexts/CalendarContext';
import { useSchedules } from '../../contexts/ScheduleContext';
import { useFriends } from '../../contexts/FriendContext';
import { useNotifications } from '../../contexts/NotificationContext';
import { ApiService } from '../../services/apiService';
import { formatDate, isToday, addDays } from '../../utils/dateUtils';

export const StatisticsPage = () => {
  const { calendars } = useCalendars();
  const { schedules } = useSchedules();
  const { acceptedFriendships } = useFriends();
  const { notifications } = useNotifications();
  
  const [statistics, setStatistics] = useState(null);
  const [selectedPeriod, setSelectedPeriod] = useState('month');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchStatistics();
  }, [selectedPeriod]);

  const fetchStatistics = async () => {
    try {
      setLoading(true);
      const response = await ApiService.getStatistics(selectedPeriod);
      setStatistics(response.data || response);
    } catch (error) {
      console.error('Failed to fetch statistics:', error);
      // 목 데이터 사용
      generateMockStatistics();
    } finally {
      setLoading(false);
    }
  };

  const generateMockStatistics = () => {
    const today = new Date();
    const weekAgo = addDays(today, -7);
    const monthAgo = addDays(today, -30);
    
    const recentSchedules = schedules.filter(schedule => 
      new Date(schedule.createdAt) >= monthAgo
    );
    
    setStatistics({
      totalSchedules: schedules.length,
      totalCalendars: calendars.length,
      totalFriends: acceptedFriendships.length,
      totalNotifications: notifications.length,
      schedulesThisWeek: schedules.filter(schedule => 
        new Date(schedule.startDateTime) >= weekAgo
      ).length,
      schedulesThisMonth: recentSchedules.length,
      dailyActivity: Array.from({ length: 7 }, (_, i) => {
        const date = addDays(today, -6 + i);
        return {
          date: formatDate(date, { month: 'short', day: 'numeric' }),
          schedules: schedules.filter(schedule => 
            new Date(schedule.startDateTime).toDateString() === date.toDateString()
          ).length
        };
      }),
      categoryBreakdown: [
        { name: '업무', count: Math.floor(schedules.length * 0.4) },
        { name: '개인', count: Math.floor(schedules.length * 0.3) },
        { name: '미팅', count: Math.floor(schedules.length * 0.2) },
        { name: '기타', count: Math.floor(schedules.length * 0.1) }
      ]
    });
  };

  const cardStyle = {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb'
  };

  const statCardStyle = (color) => ({
    ...cardStyle,
    background: `linear-gradient(135deg, ${color} 0%, ${color}dd 100%)`,
    color: 'white',
    padding: '1.5rem'
  });

  if (loading && !statistics) {
    return (
      <div style={{ textAlign: 'center', padding: '3rem' }}>
        <Activity style={{ width: '3rem', height: '3rem', color: '#2563eb', margin: '0 auto 1rem auto' }} />
        <p style={{ color: '#6b7280' }}>통계 데이터를 불러오는 중...</p>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>
            통계
          </h1>
          <p style={{ color: '#6b7280' }}>
            캘박 사용 현황을 확인하세요
          </p>
        </div>
        
        <select
          value={selectedPeriod}
          onChange={(e) => setSelectedPeriod(e.target.value)}
          style={{
            padding: '0.5rem 1rem',
            border: '1px solid #d1d5db',
            borderRadius: '0.5rem',
            backgroundColor: 'white',
            fontSize: '0.875rem'
          }}
        >
          <option value="week">이번 주</option>
          <option value="month">이번 달</option>
          <option value="quarter">이번 분기</option>
          <option value="year">올해</option>
        </select>
      </div>
      
      {/* 주요 통계 카드 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
        <div style={statCardStyle('#3b82f6')}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <h3 style={{ fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem', opacity: 0.9 }}>
                총 일정
              </h3>
              <p style={{ fontSize: '1.875rem', fontWeight: 'bold', margin: 0 }}>
                {statistics?.totalSchedules || schedules.length}
              </p>
            </div>
            <CalendarIcon style={{ width: '2.5rem', height: '2.5rem', opacity: 0.8 }} />
          </div>
        </div>
        
        <div style={statCardStyle('#10b981')}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <h3 style={{ fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem', opacity: 0.9 }}>
                이번 주 일정
              </h3>
              <p style={{ fontSize: '1.875rem', fontWeight: 'bold', margin: 0 }}>
                {statistics?.schedulesThisWeek || 0}
              </p>
            </div>
            <TrendingUp style={{ width: '2.5rem', height: '2.5rem', opacity: 0.8 }} />
          </div>
        </div>
        
        <div style={statCardStyle('#8b5cf6')}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <h3 style={{ fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem', opacity: 0.9 }}>
                친구 수
              </h3>
              <p style={{ fontSize: '1.875rem', fontWeight: 'bold', margin: 0 }}>
                {statistics?.totalFriends || acceptedFriendships.length}
              </p>
            </div>
            <Users style={{ width: '2.5rem', height: '2.5rem', opacity: 0.8 }} />
          </div>
        </div>
        
        <div style={statCardStyle('#f59e0b')}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <h3 style={{ fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem', opacity: 0.9 }}>
                캘린더 수
              </h3>
              <p style={{ fontSize: '1.875rem', fontWeight: 'bold', margin: 0 }}>
                {statistics?.totalCalendars || calendars.length}
              </p>
            </div>
            <BarChart3 style={{ width: '2.5rem', height: '2.5rem', opacity: 0.8 }} />
          </div>
        </div>
      </div>

      {/* 상세 통계 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))', gap: '1.5rem' }}>
        {/* 일주일 활동 */}
        <div style={cardStyle}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            최근 7일 활동
          </h3>
          {statistics?.dailyActivity && (
            <div style={{ display: 'flex', alignItems: 'end', gap: '0.5rem', height: '200px' }}>
              {statistics.dailyActivity.map((day, index) => {
                const maxCount = Math.max(...statistics.dailyActivity.map(d => d.schedules));
                const height = maxCount > 0 ? (day.schedules / maxCount) * 150 : 0;
                
                return (
                  <div key={index} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                    <div
                      style={{
                        width: '100%',
                        height: `${height}px`,
                        backgroundColor: '#3b82f6',
                        borderRadius: '4px 4px 0 0',
                        marginBottom: '0.5rem',
                        minHeight: '4px'
                      }}
                      title={`${day.schedules}개 일정`}
                    />
                    <span style={{ fontSize: '0.75rem', color: '#6b7280', textAlign: 'center' }}>
                      {day.date}
                    </span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* 카테고리별 분석 */}
        <div style={cardStyle}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            일정 카테고리 분석
          </h3>
          {statistics?.categoryBreakdown && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {statistics.categoryBreakdown.map((category, index) => {
                const total = statistics.categoryBreakdown.reduce((sum, cat) => sum + cat.count, 0);
                const percentage = total > 0 ? (category.count / total) * 100 : 0;
                const colors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'];
                
                return (
                  <div key={index}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                      <span style={{ fontSize: '0.875rem', fontWeight: '500' }}>{category.name}</span>
                      <span style={{ fontSize: '0.875rem', color: '#6b7280' }}>
                        {category.count}개 ({percentage.toFixed(1)}%)
                      </span>
                    </div>
                    <div style={{
                      width: '100%',
                      height: '8px',
                      backgroundColor: '#f3f4f6',
                      borderRadius: '4px',
                      overflow: 'hidden'
                    }}>
                      <div style={{
                        width: `${percentage}%`,
                        height: '100%',
                        backgroundColor: colors[index % colors.length],
                        borderRadius: '4px'
                      }} />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};