// AppRouter.js
import React, { useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import { useLocation, useNavigate, Navigate } from "react-router-dom";
import { LoadingSpinner } from "./common/LoadingSpinner";
import { LoginPage } from "./pages/LoginPage";
import { SignupCompletePage } from "./pages/SignupCompletePage";
import { Dashboard } from "./pages/Dashboard";
import { Sidebar } from "./layout/Sidebar";
import { SchedulesPage } from "./pages/SchedulesPage";
import { FriendsPage } from "./pages/FriendsPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { StatisticsPage } from "./pages/StatisticsPage";
import { SearchPage } from "./pages/SearchPage";
import { CalendarPage } from "./pages/CalendarPage";

export const AppRouter = () => {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  // ✅ /login → /dashboard 이동
  useEffect(() => {
    if (!loading && isAuthenticated && location.pathname === "/login") {
      navigate("/dashboard", { replace: true });
    }
  }, [isAuthenticated, loading, location.pathname, navigate]);

  if (loading) {
    return (
      <div
        style={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          backgroundColor: "#f9fafb",
        }}
      >
        <LoadingSpinner size="3rem" text="로딩 중..." />
      </div>
    );
  }

  // ✅ /login/success 들어오면 바로 대시보드로 리다이렉트
  if (isAuthenticated && location.pathname === "/login/success") {
    return <Navigate to="/dashboard" replace />;
  }

  // ✅ 신규회원 추가정보 입력
  if (location.pathname === "/signup/complete") {
    return <SignupCompletePage />;
  }

  // ✅ 인증 안된 상태 → 로그인 페이지
  if (!isAuthenticated) {
    return <LoginPage />;
  }

  // ✅ 메인 레이아웃
  const mainLayoutStyle = {
    display: "flex",
    height: "100vh",
    backgroundColor: "#f9fafb",
  };

  const contentStyle = {
    flex: 1,
    overflow: "auto",
    padding: "2rem",
  };

  return (
    <div style={mainLayoutStyle}>
      <Sidebar />
      <div style={contentStyle}>
        {location.pathname === "/dashboard" && <Dashboard />}
        {location.pathname === "/calendar" && <CalendarPage />}
        {location.pathname === "/schedules" && <SchedulesPage />}
        {location.pathname === "/friends" && <FriendsPage />}
        {location.pathname === "/notifications" && <NotificationsPage />}
        {location.pathname === "/statistics" && <StatisticsPage />}
        {location.pathname === "/search" && <SearchPage />}
        {location.pathname === "/" && <Dashboard />}
      </div>
    </div>
  );
};
