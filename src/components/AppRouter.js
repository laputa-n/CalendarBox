import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useLocation, useNavigate } from 'react-router-dom';
import { LoadingSpinner } from './common/LoadingSpinner';
import { LoginPage } from './pages/LoginPage';
import { SignupCompletePage } from './pages/SignupCompletePage';
import { Dashboard } from './pages/Dashboard';
import { Sidebar } from './layout/Sidebar';
import { SchedulesPage } from './pages/SchedulesPage';
import { FriendsPage } from './pages/FriendsPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { StatisticsPage } from './pages/StatisticsPage';
import { SearchPage } from './pages/SearchPage';
import { CalendarPage } from './pages/CalendarPage';
import { CallbackPage } from './pages/CallbackPage';

export const AppRouter = () => {
  const { isAuthenticated, loading } = useAuth(); // user 대신 isAuthenticated 사용
  const location = useLocation();
  const navigate = useNavigate();

  if (loading) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#f9fafb'
      }}>
        <LoadingSpinner size="3rem" text="로딩 중..." />
      </div>
    );
  }

  // 인증이 필요없는 페이지들
  if (location.pathname === '/signup/complete') {
    return <SignupCompletePage />;
  }

  if (location.pathname === '/auth/callback') {
    return <CallbackPage />;
  }

  if (!isAuthenticated) { // user 대신 isAuthenticated 사용
    return <LoginPage />;
  }

  if (location.pathname === '/login') {
    navigate('/dashboard', { replace: true });
    return null;
  }

  const mainLayoutStyle = {
    display: 'flex',
    height: '100vh',
    backgroundColor: '#f9fafb'
  };

  const contentStyle = {
    flex: 1,
    overflow: 'auto',
    padding: '2rem'
  };

  return (
    <div style={mainLayoutStyle}>
      <Sidebar />
      <div style={contentStyle}>
        {location.pathname === '/dashboard' && <Dashboard />}
        {location.pathname === '/calendar' && <CalendarPage />}
        {location.pathname === '/schedules' && <SchedulesPage />}
        {location.pathname === '/friends' && <FriendsPage />}
        {location.pathname === '/notifications' && <NotificationsPage />}
        {location.pathname === '/statistics' && <StatisticsPage />}
        {location.pathname === '/search' && <SearchPage />}
        {location.pathname === '/' && <Dashboard />}
      </div>
    </div>
  );
};