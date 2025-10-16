import React from 'react';
import { Loader2 } from 'lucide-react';

export const LoadingSpinner = ({ size = '2rem', text = '로딩 중...' }) => (
  <div style={{
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '2rem'
  }}>
    <Loader2 style={{ 
      width: size, 
      height: size, 
      color: '#2563eb',
      animation: 'spin 1s linear infinite'
    }} />
    <p style={{ color: '#6b7280', marginTop: '1rem' }}>{text}</p>
  </div>
);
