// CallbackPage.js 수정
import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

export const CallbackPage = () => {
  const navigate = useNavigate();
  const hasProcessed = useRef(false);

  useEffect(() => {
    if (hasProcessed.current) return;
    hasProcessed.current = true;

    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    const nextAction = urlParams.get('nextAction');
    const email = urlParams.get('email');

    console.log('CallbackPage - token:', token);
    console.log('CallbackPage - nextAction:', nextAction);
    console.log('CallbackPage - email:', email);

    if (nextAction === 'COMPLETE_PROFILE' && token) {
      console.log('신규 회원 - 회원가입 페이지로 이동');
      localStorage.setItem('signupToken', token);
      localStorage.setItem('signupEmail', email);
      navigate('/signup/complete', { replace: true });
    } else if (token) {
      console.log('기존 회원 - 대시보드로 이동');
      localStorage.setItem('authToken', token);
      
      // 페이지 새로고침으로 AuthContext 재초기화
      window.location.href = '/dashboard';
    } else {
      console.log('토큰 없음 - 로그인 페이지로 이동');
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}>
      <div>처리 중...</div>
    </div>
  );
};