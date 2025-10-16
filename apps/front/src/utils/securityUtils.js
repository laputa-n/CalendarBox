export const sanitizeInput = (input) => {
  if (typeof input !== 'string') return input;
  
  return input
    .replace(/[<>]/g, '') // HTML 태그 제거
    .replace(/javascript:/gi, '') // JavaScript 스키마 제거
    .replace(/on\w+=/gi, '') // 이벤트 핸들러 제거
    .trim();
};

export const validateCSRFToken = () => {
  const token = document.querySelector('meta[name="csrf-token"]')?.getAttribute('content');
  return token || '';
};

export const encryptSensitiveData = (data, key) => {
  // 실제 구현에서는 Web Crypto API 사용
  // 여기서는 간단한 base64 인코딩만 예시로 제공
  try {
    return btoa(JSON.stringify(data));
  } catch (error) {
    console.error('Encryption failed:', error);
    return data;
  }
};

export const decryptSensitiveData = (encryptedData, key) => {
  try {
    return JSON.parse(atob(encryptedData));
  } catch (error) {
    console.error('Decryption failed:', error);
    return null;
  }
};

export const generateCSP = () => {
  return {
    'default-src': "'self'",
    'script-src': "'self' 'unsafe-inline' https://t1.kakaocdn.net",
    'style-src': "'self' 'unsafe-inline'",
    'img-src': "'self' data: https:",
    'connect-src': "'self' https://api.calbox.com wss://api.calbox.com",
    'font-src': "'self'",
    'object-src': "'none'",
    'base-uri': "'self'",
    'form-action': "'self'"
  };
};