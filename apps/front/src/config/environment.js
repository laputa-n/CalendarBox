export const ENV_CONFIG = {
  development: {
    API_BASE_URL: 'http://localhost:3001/api',
    KAKAO_CLIENT_ID: 'your-dev-kakao-client-id',
    LOG_LEVEL: 'debug'
  },
  staging: {
    API_BASE_URL: 'https://api-staging.calbox.com/api',
    KAKAO_CLIENT_ID: 'your-staging-kakao-client-id',
    LOG_LEVEL: 'info'
  },
  production: {
    API_BASE_URL: 'https://api.calbox.com/api',
    KAKAO_CLIENT_ID: 'your-prod-kakao-client-id',
    LOG_LEVEL: 'error'
  }
};

export const getConfig = () => {
  const env = process.env.NODE_ENV || 'development';
  return ENV_CONFIG[env];
};
