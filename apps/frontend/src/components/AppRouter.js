// AppRouter.js
import React, { useEffect } from "react";
import { Routes, Route, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
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
import { CalendarBoardPage } from "./pages/CalendarBoardPage";
import { CalendarDetailPage } from "./pages/CalendarDetailPage";
import {CalendarMemberList} from "./pages/CalendarMemberList";

export const AppRouter = () => {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  // ✅ 로그인 후 /login → /dashboard 리다이렉트
  useEffect(() => {
    if (!loading && isAuthenticated && location.pathname === "/login") {
      navigate("/dashboard", { replace: true });
    }
  }, [isAuthenticated, loading, location.pathname, navigate]);

  /* =========================
   *  Loading
   * ========================= */
  if (loading) {
    return (
      <div style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        backgroundColor: "#f9fafb",
      }}>
        <LoadingSpinner size="3rem" text="로딩 중..." />
      </div>
    );
  }

  /* =========================
   *  인증 전 라우트
   * ========================= */
  if (!isAuthenticated) {
    return (
      <Routes>
        <Route path="/signup/complete" element={<SignupCompletePage />} />
        <Route path="*" element={<LoginPage />} />
      </Routes>
    );
  }

  /* =========================
   *  인증 후 메인 레이아웃
   * ========================= */
  return (
    <div style={{
      display: "flex",
      height: "100vh",
      backgroundColor: "#f9fafb",
    }}>
      <Sidebar />

      <div style={{
        flex: 1,
        overflow: "auto",
        padding: "2rem",
      }}>
        <Routes>
          {/* 기본 */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />

          {/* 일반 페이지 */}
          <Route path="/schedules" element={<SchedulesPage />} />
          <Route path="/friends" element={<FriendsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/statistics" element={<StatisticsPage />} />
          <Route path="/search" element={<SearchPage />} />

          {/* 캘린더 */}
          <Route path="/calendar" element={<Navigate to="/calendar/board" replace />} />
          <Route path="/calendar/board" element={<CalendarBoardPage />} />
          <Route path="/calendar/:calendarId" element={<CalendarPage />} />
          <Route path="/calendar/:calendarId/detail" element={<CalendarDetailPage />} />
          <Route path="/calendar/:calendarId/members" element={<CalendarMemberList />}
/>
          {/* fallback */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </div>
    </div>
  );
};
