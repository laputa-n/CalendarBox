// src/services/apiService.js

// ⛳️ 백엔드가 /api 프리픽스를 안 쓰면 development를 'http://localhost:8080' 로 바꾸세요.
const API_CONFIG = {
  development: 'http://localhost:8080',
  staging: 'https://api-staging.calbox.com/api',
  production: 'https://api.calbox.com/api',
};

const API_BASE_URL = API_CONFIG[process.env.NODE_ENV] || API_CONFIG.development;

export class ApiService {
  static getAuthToken() {
    return localStorage.getItem('authToken');
  }

  static setAuthToken(token) {
    localStorage.setItem('authToken', token);
  }

  static removeAuthToken() {
    localStorage.removeItem('authToken');
  }

  /**
   * request(endpoint, { method, headers, body, allowUnauthed })
   * - credentials:'include' 로 httpOnly 쿠키 전송
   * - allowUnauthed=true 이면 401/403을 예외로 던지지 않고 결과로 반환
   */
  static async request(endpoint, options = {}) {
    const token = this.getAuthToken();
    const { allowUnauthed = false, headers = {}, ...rest } = options;

    const hasBody = rest.body !== undefined && rest.body !== null;

    const config = {
      credentials: 'include', // ★ 쿠키 포함
      ...rest,
      headers: {
        ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
    };

    const res = await fetch(`${API_BASE_URL}${endpoint}`, config);

    if (!res.ok) {
      // 상태 API 등에서 미인증을 정상 흐름으로 처리
      if (allowUnauthed && (res.status === 401 || res.status === 403)) {
        const ct = res.headers.get('content-type') || '';
        if (ct.includes('application/json')) {
          const data = await res.json().catch(() => ({}));
          // 서버가 {authenticated:false} 등 반환하면 그대로 전달
          return data?.authenticated !== undefined ? data : { authenticated: false };
        }
        return { authenticated: false };
      }

      if (res.status === 401) {
        this.removeAuthToken();
        const txt = await res.text().catch(() => '');
        throw new Error(txt || '인증이 만료되었습니다. 다시 로그인해주세요.');
      }

      const ct = res.headers.get('content-type') || '';
      const errorData = ct.includes('application/json') ? await res.json().catch(() => ({})) : {};
      throw new Error(errorData.message || `HTTP error! status: ${res.status}`);
    }

    const contentType = res.headers.get('content-type') || '';
    return contentType.includes('application/json') ? await res.json() : res;
  }

  // === 인증 관련 ===

  // 카카오 로그인 URL 생성
  static getKakaoLoginUrl() {
    return `${API_BASE_URL}/auth/kakao/login`;
  }

  // 카카오 콜백 처리
  static async handleKakaoCallback(code) {
    return this.request('/auth/kakao/callback', {
      method: 'POST',
      body: JSON.stringify({ code }),
    });
  }

  // 회원가입 완료 (signupToken 사용)
static async completeSignup(signupData, signupToken) {
  return this.request('/auth/signup/complete', {
    method: 'POST',
    headers: {
      'X-Signup-Token': signupToken,
    },
    body: JSON.stringify({
      name: signupData.name,
      phoneNumber: signupData.phoneNumber
      // nickname 제거 (백엔드에서 받지 않음)
    }),
  });
}
  // ★ 상태 API: 미인증(401/403)도 throw하지 않고 {authenticated:false} 반환
  static async getAuthStatus() {
    return this.request('/auth/me', { allowUnauthed: true });
  }

  static async getUserProfile() {
    return this.request('/auth/profile');
  }

  static async updateUserProfile(profileData) {
    return this.request('/auth/profile', {
      method: 'PUT',
      body: JSON.stringify(profileData),
    });
  }

  static async refreshToken() {
    return this.request('/auth/refresh', { method: 'POST' });
  }

  // === 친구 ===
  static async getFriendships() {
    return this.request('/friendships/received');
  }

  static async sendFriendRequest(friendEmail) {
    return this.request('/friendships/request', {
      method: 'POST',
      body: JSON.stringify({ friendEmail }),
    });
  }

  static async acceptFriendship(friendshipId) {
    return this.request(`/friendships/${friendshipId}`, {
      method: 'PATCH',
      body: JSON.stringify({ status: 'accepted' }),
    });
  }

  static async rejectFriendship(friendshipId) {
    return this.request(`/friendships/${friendshipId}`, {
      method: 'PATCH',
      body: JSON.stringify({ status: 'rejected' }),
    });
  }

  static async removeFriend(friendshipId) {
    return this.request(`/friendships/${friendshipId}`, { method: 'DELETE' });
  }

  // === 캘린더 ===
  static async getCalendars() {
    return this.request('/calendars');
  }

  static async createCalendar(calendarData) {
    return this.request('/calendars', {
      method: 'POST',
      body: JSON.stringify(calendarData),
    });
  }

  static async updateCalendar(calendarId, calendarData) {
    return this.request(`/calendars/${calendarId}`, {
      method: 'PUT',
      body: JSON.stringify(calendarData),
    });
  }

  static async deleteCalendar(calendarId) {
    return this.request(`/calendars/${calendarId}`, { method: 'DELETE' });
  }

  static async shareCalendar(calendarId, shareData) {
    return this.request(`/calendars/${calendarId}/share`, {
      method: 'POST',
      body: JSON.stringify(shareData),
    });
  }

  // === 스케줄 ===
  static async getSchedules(calendarId, params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const endpoint = queryString
      ? `/calendars/${calendarId}/schedules?${queryString}`
      : `/calendars/${calendarId}/schedules`;
    return this.request(endpoint);
  }

  static async createSchedule(calendarId, scheduleData) {
    return this.request(`/calendars/${calendarId}/schedules`, {
      method: 'POST',
      body: JSON.stringify(scheduleData),
    });
  }

  static async updateSchedule(scheduleId, scheduleData) {
    return this.request(`/schedules/${scheduleId}`, {
      method: 'PUT',
      body: JSON.stringify(scheduleData),
    });
  }

  static async deleteSchedule(scheduleId) {
    return this.request(`/schedules/${scheduleId}`, { method: 'DELETE' });
  }

  static async searchSchedules(query, filters = {}) {
    const params = new URLSearchParams({ query, ...filters });
    return this.request(`/schedules/search?${params}`);
  }

  static async getScheduleById(scheduleId) {
    return this.request(`/schedules/${scheduleId}`);
  }

  // === 일정 참여자 ===
  static async inviteToSchedule(scheduleId, inviteData) {
    return this.request(`/schedules/${scheduleId}/invite`, {
      method: 'POST',
      body: JSON.stringify(inviteData),
    });
  }

  static async respondToInvitation(invitationId, response) {
    return this.request(`/invitations/${invitationId}/respond`, {
      method: 'PATCH',
      body: JSON.stringify({ response }),
    });
  }

  // === 알림 ===
  static async getNotifications(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const endpoint = queryString ? `/notifications?${queryString}` : '/notifications';
    return this.request(endpoint);
  }

  static async markNotificationAsRead(notificationId) {
    return this.request(`/notifications/${notificationId}/read`, { method: 'PATCH' });
  }

  static async markAllNotificationsAsRead() {
    return this.request('/notifications/read-all', { method: 'PATCH' });
  }

  static async deleteNotification(notificationId) {
    return this.request(`/notifications/${notificationId}`, { method: 'DELETE' });
  }

  // === 통계 ===
  static async getStatistics(period = 'month') {
    return this.request(`/statistics?period=${period}`);
  }

  static async getUserActivity() {
    return this.request('/statistics/activity');
  }
}