// components/pages/SignupCompletePage.js
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

export const SignupCompletePage = () => {
  const navigate = useNavigate();
  const { completeSignup, loginExistingMember } = useAuth();
  const [loading, setLoading] = useState(false);
  const [email, setEmail] = useState('');
  const [memberType, setMemberType] = useState(''); // 'new' 또는 'existing'
  const [formData, setFormData] = useState({
    name: '',
    phoneNumber: '',
    nickname: ''
  });

  useEffect(() => {
    const signupEmail = localStorage.getItem('signupEmail');
    const signupToken = localStorage.getItem('signupToken');
    
    if (!signupToken) {
      navigate('/login');
      return;
    }
    
    setEmail(signupEmail || '');
  }, [navigate]);

  const handleMemberTypeSelect = (type) => {
    setMemberType(type);
  };

  const handleExistingMember = async () => {
    try {
      console.log('=== 기존회원 로그인 시작 ===');
      await loginExistingMember();
      console.log('=== 기존회원 로그인 완료 ===');
      navigate('/dashboard');
    } catch (error) {
      console.error('=== 기존회원 로그인 실패 ===', error);
      navigate('/login');
    }
  };

  const handleNewMemberSubmit = async (e) => {
    e.preventDefault();
    
    try {
      setLoading(true);
      
      // signupToken 가져오기 및 검증
      const signupToken = localStorage.getItem('signupToken');
      
      if (!signupToken) {
        throw new Error('회원가입 토큰이 없습니다.');
      }
      
      console.log('=== 회원가입 시작 ===');
      console.log('폼 데이터:', formData);
      console.log('signupToken:', signupToken);
      
      // signupToken을 두 번째 인자로 전달
      const result = await completeSignup(formData, signupToken);
      
      console.log('=== 회원가입 완료 ===');
      console.log('결과:', result);
      console.log('저장된 authToken:', localStorage.getItem('authToken'));
      
      navigate('/dashboard');
    } catch (error) {
      console.error('=== 회원가입 실패 ===');
      console.error('에러:', error);
    } finally {
      setLoading(false);
    }
  };

  const containerStyle = {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#f9fafb',
    padding: '1rem'
  };

  const cardStyle = {
    backgroundColor: 'white',
    padding: '2rem',
    borderRadius: '0.5rem',
    boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
    width: '100%',
    maxWidth: '28rem'
  };

  const buttonStyle = {
    width: '100%',
    padding: '1rem',
    borderRadius: '0.375rem',
    border: '1px solid #d1d5db',
    backgroundColor: 'white',
    cursor: 'pointer',
    fontSize: '1rem',
    marginBottom: '1rem',
    transition: 'all 0.2s'
  };

  const selectedButtonStyle = {
    ...buttonStyle,
    backgroundColor: '#4f46e5',
    color: 'white',
    borderColor: '#4f46e5'
  };

  // 회원 유형 선택 화면
  if (!memberType) {
    return (
      <div style={containerStyle}>
        <div style={cardStyle}>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem', textAlign: 'center' }}>
            회원 유형을 선택해주세요
          </h2>
          <p style={{ textAlign: 'center', color: '#6b7280', marginBottom: '2rem' }}>
            {email}
          </p>
          
          <button
            style={buttonStyle}
            onClick={() => handleMemberTypeSelect('new')}
            onMouseOver={(e) => e.target.style.backgroundColor = '#f3f4f6'}
            onMouseOut={(e) => e.target.style.backgroundColor = 'white'}
          >
            <div style={{ textAlign: 'left' }}>
              <h3 style={{ fontWeight: '600', marginBottom: '0.25rem' }}>신규 회원</h3>
              <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: 0 }}>
                처음 서비스를 이용합니다
              </p>
            </div>
          </button>

          <button
            style={buttonStyle}
            onClick={() => handleMemberTypeSelect('existing')}
            onMouseOver={(e) => e.target.style.backgroundColor = '#f3f4f6'}
            onMouseOut={(e) => e.target.style.backgroundColor = 'white'}
          >
            <div style={{ textAlign: 'left' }}>
              <h3 style={{ fontWeight: '600', marginBottom: '0.25rem' }}>기존 회원</h3>
              <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: 0 }}>
                이미 가입된 계정입니다
              </p>
            </div>
          </button>
        </div>
      </div>
    );
  }

  // 기존 회원 확인 화면
  if (memberType === 'existing') {
    return (
      <div style={containerStyle}>
        <div style={cardStyle}>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem', textAlign: 'center' }}>
            기존 회원 확인
          </h2>
          <p style={{ textAlign: 'center', color: '#6b7280', marginBottom: '2rem' }}>
            {email} 계정으로 로그인합니다
          </p>
          
          <button
            onClick={handleExistingMember}
            style={{
              width: '100%',
              backgroundColor: '#4f46e5',
              color: 'white',
              padding: '0.75rem',
              borderRadius: '0.375rem',
              border: 'none',
              fontSize: '1rem',
              fontWeight: '500',
              cursor: 'pointer',
              marginBottom: '0.5rem'
            }}
          >
            로그인 하기
          </button>
          
          <button
            onClick={() => setMemberType('')}
            style={{
              width: '100%',
              backgroundColor: 'transparent',
              color: '#6b7280',
              padding: '0.75rem',
              borderRadius: '0.375rem',
              border: '1px solid #d1d5db',
              fontSize: '0.875rem',
              cursor: 'pointer'
            }}
          >
            뒤로 가기
          </button>
        </div>
      </div>
    );
  }

  // 신규 회원 정보 입력 화면
  return (
    <div style={containerStyle}>
      <div style={cardStyle}>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 'bold', marginBottom: '1rem', textAlign: 'center' }}>
          신규 회원 정보 입력
        </h2>
        
        <form onSubmit={handleNewMemberSubmit}>
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
              이메일
            </label>
            <input
              type="email"
              value={email}
              disabled
              style={{
                width: '100%',
                padding: '0.75rem',
                border: '1px solid #d1d5db',
                borderRadius: '0.375rem',
                backgroundColor: '#f9fafb'
              }}
            />
          </div>
          
          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
              이름 *
            </label>
            <input
              type="text"
              required
              value={formData.name}
              onChange={(e) => setFormData({...formData, name: e.target.value})}
              placeholder="실명을 입력해주세요"
              style={{
                width: '100%',
                padding: '0.75rem',
                border: '1px solid #d1d5db',
                borderRadius: '0.375rem'
              }}
            />
          </div>

          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
              닉네임 *
            </label>
            <input
              type="text"
              required
              value={formData.nickname}
              onChange={(e) => setFormData({...formData, nickname: e.target.value})}
              placeholder="다른 사용자에게 표시될 이름"
              style={{
                width: '100%',
                padding: '0.75rem',
                border: '1px solid #d1d5db',
                borderRadius: '0.375rem'
              }}
            />
          </div>
          
          <div style={{ marginBottom: '1.5rem' }}>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
              전화번호 *
            </label>
            <input
              type="tel"
              required
              value={formData.phoneNumber}
              onChange={(e) => setFormData({...formData, phoneNumber: e.target.value})}
              placeholder="010-1234-5678"
              style={{
                width: '100%',
                padding: '0.75rem',
                border: '1px solid #d1d5db',
                borderRadius: '0.375rem'
              }}
            />
          </div>
          
          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              backgroundColor: '#4f46e5',
              color: 'white',
              padding: '0.75rem',
              borderRadius: '0.375rem',
              border: 'none',
              fontSize: '1rem',
              fontWeight: '500',
              cursor: loading ? 'not-allowed' : 'pointer',
              opacity: loading ? 0.6 : 1,
              marginBottom: '0.5rem'
            }}
          >
            {loading ? '처리 중...' : '회원가입 완료'}
          </button>

          <button
            type="button"
            onClick={() => setMemberType('')}
            style={{
              width: '100%',
              backgroundColor: 'transparent',
              color: '#6b7280',
              padding: '0.75rem',
              borderRadius: '0.375rem',
              border: '1px solid #d1d5db',
              fontSize: '0.875rem',
              cursor: 'pointer'
            }}
          >
            뒤로 가기
          </button>
        </form>
      </div>
    </div>
  );
};