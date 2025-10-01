// contexts/AuthContext.js
import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { ApiService } from '../services/apiService';
import { useError } from './ErrorContext';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const { showError } = useError();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const bootRef = useRef(false);

  useEffect(() => {
    if (bootRef.current) return;
    bootRef.current = true;
    initializeAuth();
  }, []);

  /** 🔹 앱 시작 시 인증 상태 확인 (/auth/me) */
  const initializeAuth = async () => {
    try {
      setLoading(true);
      console.log('[Auth] initializeAuth 실행');

      const data = await ApiService.getAuthStatus(); // 👉 /api/auth/me 호출
      console.log('[Auth] /auth/me 응답:', data);

      const memberData = data?.member || data;

       if (memberData) {
      setUser(memberData);
      setIsAuthenticated(true);
    } else {
      setUser(null);
      setIsAuthenticated(false);
    }
  } catch (error) {
    console.error('[Auth] initializeAuth 에러:', error);
    setUser(null);
    setIsAuthenticated(false);
  } finally {
    setLoading(false);
  }
};
  /** 🔹 카카오 로그인 시작 */
  const startKakaoLogin = () => {
    window.location.href = ApiService.getKakaoLoginUrl();
  };

  /** 🔹 회원가입 완료 → 쿠키 기반이라 /auth/me 다시 호출 */
  const completeSignup = async (profileData) => {
    try {
      setLoading(true);
      console.log('[Auth] completeSignup 요청:', profileData);

      const response = await ApiService.completeSignup(profileData);
      console.log('[Auth] completeSignup 응답:', response);

      await initializeAuth(); // 쿠키에 토큰 들어갔는지 확인
      return response;
    } catch (error) {
      console.error('[Auth] completeSignup 에러:', error);
      showError(error.message || '회원가입에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  /** 🔹 로그아웃 */
  const logout = async () => {
    try {
      setLoading(true);
      await ApiService.logout(); // 백엔드에 쿠키 삭제 요청
    } catch (error) {
      console.error('[Auth] logout 에러:', error);
    } finally {
      setUser(null);
      setIsAuthenticated(false);
      setLoading(false);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAuthenticated,
        startKakaoLogin,
        completeSignup,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
};
