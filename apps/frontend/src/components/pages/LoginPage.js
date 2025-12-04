// components/pages/LoginPage.js
import React, { useState } from 'react';
import { Calendar, Loader2 } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import {ApiService} from "../../services/apiService";

export const LoginPage = () => {
  const { startKakaoLogin } = useAuth();
  const [loading, setLoading] = useState(false);

  const handleKakaoLogin = async () => {
  try {
    setLoading(true);
   window.location.href = 'http://localhost:8080/api/auth/kakao/login';
  //window.location.href = ApiService.getKakaoLoginUrl();
  } catch (error) {
    console.error('Login failed:', error);
  } finally {
    setLoading(false);
  }
};

  // 스타일 정의 추가
  const loginContainerStyle = {
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #dbeafe 0%, #e0e7ff 100%)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '1rem'
  };

  const loginCardStyle = {
    backgroundColor: 'white',
    padding: '2.5rem',
    borderRadius: '1rem',
    boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
    width: '100%',
    maxWidth: '24rem',
    textAlign: 'center'
  };

  const loginButtonStyle = {
    width: '100%',
    backgroundColor: '#fbbf24',
    color: 'black',
    fontWeight: '500',
    padding: '1rem 1.5rem',
    borderRadius: '0.75rem',
    border: 'none',
    cursor: 'pointer',
    fontSize: '1.125rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    transition: 'background-color 0.2s',
    marginTop: '1rem',
    opacity: loading ? 0.6 : 1
  };

  return (
    <div style={loginContainerStyle}>
      <div style={loginCardStyle}>
        <Calendar style={{ width: '5rem', height: '5rem', color: '#4f46e5', margin: '0 auto 1.5rem auto' }} />
        <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>
          캘박 Calendar 
        </h1>
        <p style={{ color: '#6b7280', marginBottom: '2rem' }}>
          모든 것을 기록하는 곳 캘박!!
        </p>
        <button
          onClick={handleKakaoLogin}
          disabled={loading}
          style={loginButtonStyle}
          onMouseOver={(e) => !loading && (e.target.style.backgroundColor = '#f59e0b')}
          onMouseOut={(e) => !loading && (e.target.style.backgroundColor = '#fbbf24')}
        >
          {loading ? (
            <>
              <Loader2 style={{ width: '1.25rem', height: '1.25rem', animation: 'spin 1s linear infinite' }} />
              로그인 중...
            </>
          ) : (
            <>
              <span style={{ marginRight: '0.75rem', fontSize: '1.25rem' }}>💬</span>
              카카오로 로그인
            </>
          )}
        </button>
      </div>
    </div>
  );
};