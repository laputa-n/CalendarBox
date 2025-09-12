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
  const [signupToken, setSignupToken] = useState(null); // 신규 회원용
  const bootRef = useRef(false);

  useEffect(() => {
    if (bootRef.current) return;
    bootRef.current = true;
    initializeAuth();
  }, []);

  const initializeAuth = async () => {
  try {
    console.log('=== initializeAuth 시작 ===');
    setLoading(true);
    const token = ApiService.getAuthToken();
    console.log('localStorage에서 가져온 토큰:', token);
    
    if (token) {
      try {
        const data = await ApiService.getAuthStatus();
        console.log('getAuthStatus 결과:', data);
        if (data?.authenticated) {
          setUser(data.member || data.user);
          setIsAuthenticated(true);
          console.log('인증 성공 - 사용자 설정 완료');
        } else {
          console.log('인증 실패 - 토큰 제거');
          ApiService.removeAuthToken();
          setUser(null);
          setIsAuthenticated(false);
        }
      } catch (error) {
        console.log('getAuthStatus 에러:', error);
        // /auth/me가 없으니까 에러 발생, 토큰만 있으면 인증된 것으로 처리
        setIsAuthenticated(true);
        console.log('토큰 존재로 인증 처리');
      }
    } else {
      console.log('토큰 없음 - 미인증 상태');
      setUser(null);
      setIsAuthenticated(false);
    }
  } catch (error) {
    console.error('initializeAuth 에러:', error);
    setUser(null);
    setIsAuthenticated(false);
  } finally {
    console.log('=== initializeAuth 완료 ===');
    setLoading(false);
  }
};


  const handleTokenRefresh = async () => {
    try {
      const response = await ApiService.refreshToken();
      if (response.data) {
        ApiService.setAuthToken(response.data.accessToken);
        setUser(response.data.member);
        setIsAuthenticated(true);
      }
    } catch (error) {
      // 리프레시 실패 시 로그아웃
      logout();
    }
  };

  // 카카오 로그인 시작
  const startKakaoLogin = () => {
    window.location.href = ApiService.getKakaoLoginUrl();
  };

  // 카카오 콜백 처리
  const handleKakaoCallback = async (callbackData) => {  // 매개변수 추가
  try {
    setLoading(true);
    
    if (callbackData.nextAction === 'COMPLETE_PROFILE') {
      // 신규 회원
      setSignupToken(callbackData.token);
      return { isNewMember: true, email: callbackData.email };
    } else {
      // 기존 회원 로직
      ApiService.setAuthToken(callbackData.token);
      setUser(callbackData.member);
      setIsAuthenticated(true);
      return { isNewMember: false };
    }
  } catch (error) {
    showError(error.message || '로그인에 실패했습니다.');
    throw error;
  } finally {
    setLoading(false);
  }
};

  // 회원가입 완료
const completeSignup = async (profileData) => {
  try {
    setLoading(true);
    // localStorage에서 signupToken 가져오기
    const token = localStorage.getItem('signupToken');
    
    if (!token) {
      throw new Error('회원가입 토큰이 없습니다.');
    }
    
    console.log('signupToken 사용:', token);
    const response = await ApiService.completeSignup(profileData, token);
    
    ApiService.setAuthToken(response.data.accessToken);
    setUser(response.data.member);
    setIsAuthenticated(true);
    setSignupToken(null);
    
    // localStorage에서 임시 토큰 제거
    localStorage.removeItem('signupToken');
    localStorage.removeItem('signupEmail');
    
    return response;
  } catch (error) {
    showError(error.message || '회원가입에 실패했습니다.');
    throw error;
  } finally {
    setLoading(false);
  }
};

  const logout = async () => {
    try {
      setLoading(true);
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
      setIsAuthenticated(false);
      setSignupToken(null);
      ApiService.removeAuthToken();
      setLoading(false);
    }
  };

const loginExistingMember = async () => {
  try {
    setLoading(true);
    const token = localStorage.getItem('signupToken');
    
    if (!token) {
      throw new Error('로그인 토큰이 없습니다.');
    }

    // signupToken을 사용해서 실제 accessToken으로 변환하는 API 호출
    // 또는 signupToken을 그대로 authToken으로 사용
    ApiService.setAuthToken(token);
    
    // 사용자 정보 가져오기
    const userInfo = await ApiService.getAuthStatus();
    if (userInfo.authenticated) {
      setUser(userInfo.member || userInfo.user);
      setIsAuthenticated(true);
    }
    
    // 임시 토큰들 정리
    localStorage.removeItem('signupToken');
    localStorage.removeItem('signupEmail');
    
  } catch (error) {
    showError(error.message || '로그인에 실패했습니다.');
    throw error;
  } finally {
    setLoading(false);
  }
};

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      isAuthenticated,
      signupToken,
      startKakaoLogin,
      handleKakaoCallback,
      completeSignup,
      loginExistingMember,
      logout
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
};