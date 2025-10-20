import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext'; // 기존 context 사용
import { useRouter } from 'react-router-dom'; // 기존 router 사용

const AssetizationApp = () => {
  const { user } = useAuth(); // 기존 인증 정보 활용
  const { navigate } = useRouter(); // 기존 라우터 활용
  
  // ... 나머지 AssetizationApp 코드
  
  return (
    <div className="App">
      {/* 기존 인증 정보 표시 */}
      {user && (
        <div style={{ padding: '10px', background: '#e3f2fd' }}>
          안녕하세요, {user.name}님
          <button onClick={() => navigate('/')} style={{ marginLeft: '10px' }}>
            메인으로 돌아가기
          </button>
        </div>
      )}
      
      {/* AssetizationApp 내용 */}
      {/* ... */}
    </div>
  );
};

export default AssetizationApp;