import React, { useState } from 'react';
import { 
  Home, Calendar, FileText, Users, Bell, BarChart3, Search, 
  User, LogOut, Menu 
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useNotifications } from '../../contexts/NotificationContext';
import { useNavigate, useLocation } from 'react-router-dom';

export const Sidebar = () => {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const { user, logout } = useAuth();
  const { unreadCount } = useNotifications();
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { id: 'dashboard', path: '/dashboard', icon: Home, label: '대시보드' },
    { id: 'calendar', path: '/calendar', icon: Calendar, label: '캘린더' },
    { id: 'schedules', path: '/schedules', icon: FileText, label: '일정 관리' },
    { id: 'friends', path: '/friends', icon: Users, label: '친구 관리' },
    { id: 'notifications', path: '/notifications', icon: Bell, label: '알림', badge: unreadCount },
    { id: 'statistics', path: '/statistics', icon: BarChart3, label: '통계' },
    { id: 'search', path: '/search', icon: Search, label: '검색' }
  ];

  const handleLogout = () => {
    logout();
    navigate('/login', true);
  };

  const sidebarStyle = {
    width: sidebarOpen ? '16rem' : '4rem',
    backgroundColor: 'white',
    borderRight: '1px solid #e5e7eb',
    height: '100vh',
    transition: 'width 0.3s',
    display: 'flex',
    flexDirection: 'column'
  };

  return (
    <div style={sidebarStyle}>
      {/* 헤더 */}
      <div style={{ padding: '1rem', borderBottom: '1px solid #e5e7eb' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          {sidebarOpen && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <div style={{
                width: '2rem',
                height: '2rem',
                backgroundColor: '#4f46e5',
                borderRadius: '0.5rem',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <Calendar style={{ width: '1.25rem', height: '1.25rem', color: 'white' }} />
              </div>
              <h1 style={{ fontSize: '1.25rem', fontWeight: 'bold', color: '#1f2937', margin: 0 }}>
                캘박
              </h1>
            </div>
          )}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            style={{
              padding: '0.5rem',
              borderRadius: '0.5rem',
              border: 'none',
              backgroundColor: 'transparent',
              cursor: 'pointer'
            }}
            onMouseOver={(e) => e.target.style.backgroundColor = '#f3f4f6'}
            onMouseOut={(e) => e.target.style.backgroundColor = 'transparent'}
          >
            <Menu style={{ width: '1.25rem', height: '1.25rem' }} />
          </button>
        </div>
      </div>

      {/* 사용자 정보 */}
      <div style={{ padding: '1rem', borderBottom: '1px solid #e5e7eb' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{
            width: '2.5rem',
            height: '2.5rem',
            backgroundColor: '#e0e7ff',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <User style={{ width: '1.5rem', height: '1.5rem', color: '#4f46e5' }} />
          </div>
          {sidebarOpen && (
            <div>
              <h3 style={{ fontWeight: '600', color: '#1f2937', fontSize: '0.875rem', margin: 0 }}>
                {user?.name}
              </h3>
              <p style={{ fontSize: '0.75rem', color: '#6b7280', margin: 0 }}>
                {user?.email}
              </p>
            </div>
          )}
        </div>
      </div>

      {/* 네비게이션 메뉴 */}
      <nav style={{ flex: 1, padding: '1rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
        {navItems.map(item => {
          const IconComponent = item.icon;
          const isActive = location.pathname === item.path;
          
          return (
            <button
              key={item.id}
              onClick={() => navigate(item.path)}
              style={{
                width: '100%',
                display: 'flex',
                alignItems: 'center',
                gap: '0.75rem',
                padding: '0.75rem',
                borderRadius: '0.5rem',
                border: isActive ? '1px solid #e0e7ff' : 'none',
                backgroundColor: isActive ? '#f0f4ff' : 'transparent',
                color: isActive ? '#4f46e5' : '#374151',
                cursor: 'pointer',
                transition: 'all 0.2s',
                fontSize: '0.875rem',
                fontWeight: '500',
                position: 'relative'
              }}
              onMouseOver={(e) => {
                if (!isActive) e.target.style.backgroundColor = '#f9fafb';
              }}
              onMouseOut={(e) => {
                if (!isActive) e.target.style.backgroundColor = 'transparent';
              }}
            >
              <IconComponent style={{ width: '1.25rem', height: '1.25rem' }} />
              {sidebarOpen && <span>{item.label}</span>}
              {item.badge > 0 && sidebarOpen && (
                <span style={{
                  marginLeft: 'auto',
                  backgroundColor: '#dc2626',
                  color: 'white',
                  fontSize: '0.75rem',
                  fontWeight: 'bold',
                  padding: '0.125rem 0.375rem',
                  borderRadius: '9999px',
                  minWidth: '1.25rem',
                  textAlign: 'center'
                }}>
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </nav>

      {/* 로그아웃 */}
      <div style={{ padding: '1rem', borderTop: '1px solid #e5e7eb' }}>
        <button
          onClick={handleLogout}
          style={{
            width: '100%',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            padding: '0.75rem',
            borderRadius: '0.5rem',
            border: 'none',
            backgroundColor: 'transparent',
            color: '#374151',
            cursor: 'pointer',
            transition: 'background-color 0.2s',
            fontSize: '0.875rem',
            fontWeight: '500'
          }}
          onMouseOver={(e) => e.target.style.backgroundColor = '#f9fafb'}
          onMouseOut={(e) => e.target.style.backgroundColor = 'transparent'}
        >
          <LogOut style={{ width: '1.25rem', height: '1.25rem' }} />
          {sidebarOpen && <span>로그아웃</span>}
        </button>
      </div>
    </div>
  );
};