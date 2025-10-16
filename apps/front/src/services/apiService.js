// src/services/apiService.js

const API_CONFIG = {
  development: 'http://localhost:8080/api',
  staging: 'https://api-staging.calbox.com/api',
  production: 'https://api.calbox.com/api',
};

const API_BASE_URL = API_CONFIG[process.env.NODE_ENV] || API_CONFIG.development;

export class ApiService {
  /**
   * 공통 fetch 래퍼
   */
  static async request(endpoint, options = {}) {
    const { headers = {}, ...rest } = options;
    const hasBody = rest.body !== undefined && rest.body !== null;

    const config = {
      credentials: 'include', // ✅ 항상 쿠키 포함
      ...rest,
      headers: {
        ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
        ...headers,
      },
    };

    const res = await fetch(`${API_BASE_URL}${endpoint}`, config);

    if (!res.ok) {
      const ct = res.headers.get('content-type') || '';
      const errorData = ct.includes('application/json')
        ? await res.json().catch(() => ({}))
        : {};
      throw new Error(errorData.message || `HTTP error! status: ${res.status}`);
    }

    const contentType = res.headers.get('content-type') || '';
    return contentType.includes('application/json') ? await res.json() : res;
  }

  // === 카카오 로그인 ===
  static getKakaoLoginUrl() {
    return `${API_BASE_URL}/auth/kakao/login`;
  }

  // === 인증 관련 ===
  static async getAuthStatus() {
    return this.request('/auth/me', { method: 'GET' });
  }

  static async completeSignup(signupData) {
    return this.request('/auth/signup/complete', {
      method: 'POST',
      body: JSON.stringify(signupData),
    });
  }

  static async getNextAction() {
    return this.request('/auth/kakao/next', {
      method: 'GET',
      credentials: 'include',
    });
  }

  static async logout() {
    return this.request('/auth/logout', { method: 'POST' });
  }

  static async refreshToken() {
    return this.request('/auth/refresh', { method: 'POST' });
  }

  // === 유저 프로필 ===
  static async getUserProfile() {
    return this.request('/auth/profile');
  }

  static async updateUserProfile(profileData) {
    return this.request('/auth/profile', {
      method: 'PUT',
      body: JSON.stringify(profileData),
    });
  }

  // === 친구 관련 ===
  static async getFriendships() {
    return this.request('/friendships/received');
  }

  static async sendFriendRequest(friendEmail) {
    return this.request('/friendships/request', {
      method: 'POST',
      body: JSON.stringify({ query: friendEmail }),
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

  static async sendFriendRequestById(addresseeId) {
    return this.request('/friendships/request', {
      method: 'POST',
      body: JSON.stringify({ query: addresseeId }),
    });
  }

  static async getReceivedFriendRequests(page = 1, size = 10, status = null) {
    let endpoint = `/friendships/received?page=${page - 1}&size=${size}`;
    if (status) endpoint += `&status=${status}`;
    return this.request(endpoint);
  }

  static async getSentFriendRequests(page = 1, size = 10) {
    return this.request(`/friendships/sent?page=${page - 1}&size=${size}`);
  }

  static async getAcceptedFriendships(page = 1, size = 20) {
    return this.request(`/friendships/accepted?page=${page - 1}&size=${size}`);
  }

  static async acceptFriendRequest(friendshipId) {
    return this.request(`/friendships/${friendshipId}`, {
      method: 'PATCH',
      body: JSON.stringify({ action: 'ACCEPT' }),
    });
  }

  static async rejectFriendRequest(friendshipId) {
    return this.request(`/friendships/${friendshipId}`, {
      method: 'PATCH',
      body: JSON.stringify({ action: 'REJECT' }),
    });
  }

  // === 캘린더 관련 API ===
static async getCalendars(page = 1, size = 20) {
  // ✅ 페이지 계산 및 안전한 URL 구성
  const endpoint = `/calendars?page=${page - 1}&size=${size}`;
  const response = await this.request(endpoint);
  return response;
}

static async createCalendar(calendarData) {
  // ✅ 요청 데이터 정리
  const requestData = {
    name: calendarData.name,
    type: calendarData.type || 'PERSONAL',
    visibility: calendarData.visibility || 'PRIVATE',
    isDefault: calendarData.isDefault ?? false,
  };

  // ✅ POST 요청
  const response = await this.request('/calendars', {
    method: 'POST',
    body: JSON.stringify(requestData),
  });
  return response;
}

static async getCalendarById(calendarId) {
  // ✅ 개별 캘린더 상세 조회
  const response = await this.request(`/calendars/${calendarId}`);
  return response;
}

static async updateCalendar(calendarId, calendarData) {
  // ✅ PUT 업데이트
  const requestData = {
    name: calendarData.name || null,
    visibility: calendarData.visibility?.toUpperCase() || null,
    type: calendarData.type?.toUpperCase() || null, // ✅ ENUM 명세 일치
  };
  const response = await this.request(`/calendars/${calendarId}`, {
    method: 'PATCH',
    body: JSON.stringify(requestData),
  });
  return response;
}

static async deleteCalendar(calendarId) {
  // ✅ DELETE
  const response = await this.request(`/calendars/${calendarId}`, {
    method: 'DELETE',
  });
  return response;
}

// === 캘린더 멤버 관련 API ===
static async inviteCalendarMembers(calendarId, memberIds) {
  const response = await this.request(`/calendars/${calendarId}/members`, {
    method: 'POST',
    body: JSON.stringify({ memberIds }),
  });
  return response;
}

static async getCalendarMembers(calendarId, page = 1, size = 10) {
  const endpoint = `/calendars/${calendarId}/members?page=${page}&size=${size}`;
  const response = await this.request(endpoint);
  return response;
}

static async respondToCalendarInvite(calendarMemberId, status) {
  const response = await this.request(`/calendar-members/${calendarMemberId}`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  });
  return response;
}

static async removeCalendarMember(calendarMemberId) {
  const response = await this.request(`/calendar-members/${calendarMemberId}`, {
    method: 'DELETE',
  });
  return response;
}

  // === 일정 관련 API ===
// === 일정 관련 API ===
static async getCalendarSchedules(calendarId, params = {}) {
  const queryString = new URLSearchParams(params).toString();
  const endpoint = queryString
    ? `/calendars/${calendarId}/schedules?${queryString}`
    : `/calendars/${calendarId}/schedules`;
  return this.request(endpoint);
}

static async getSchedules(params = {}) {
  const queryString = new URLSearchParams(params).toString();
  const endpoint = queryString ? `/schedules?${queryString}` : '/schedules';
  return this.request(endpoint);
}

static async createSchedule(calendarId, scheduleData) {
  return this.request(`/calendars/${calendarId}/schedules`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(scheduleData),
  });
}

static async patchSchedule(scheduleId, partialData) {
  return this.request(`/schedules/${scheduleId}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(partialData),
  });
}

static async deleteSchedule(scheduleId) {
  return this.request(`/schedules/${scheduleId}`, { method: 'DELETE' });
}

  // === 일정 참여자 관련 ===
  static async addScheduleParticipant(scheduleId, participantData) {
    return this.request(`/schedules/${scheduleId}/participants`, {
      method: 'POST',
      body: JSON.stringify(participantData),
    });
  }

  static async getScheduleParticipants(scheduleId) {
    return this.request(`/schedules/${scheduleId}/participants`);
  }

  static async removeScheduleParticipant(scheduleId, participantId) {
    return this.request(
      `/schedules/${scheduleId}/participants/${participantId}`,
      { method: 'DELETE' }
    );
  }

  static async respondToScheduleInvite(scheduleId, participantId, action) {
    return this.request(
      `/schedules/${scheduleId}/participants/${participantId}`,
      {
        method: 'PUT',
        body: JSON.stringify({ action }),
      }
    );
  }

  // === 장소 관련 ===
  static async searchPlaces(query) {
    return this.request(`/places/search?query=${encodeURIComponent(query)}`);
  }

  static async addSchedulePlace(scheduleId, placeData) {
    return this.request(`/schedules/${scheduleId}/places`, {
      method: 'POST',
      body: JSON.stringify(placeData),
    });
  }

  static async getSchedulePlaces(scheduleId) {
    return this.request(`/schedules/${scheduleId}/places`);
  }

  static async reorderSchedulePlaces(scheduleId, placesOrder) {
    return this.request(`/schedules/${scheduleId}/places`, {
      method: 'PUT',
      body: JSON.stringify(placesOrder),
    });
  }

  static async getSchedulePlaceById(scheduleId, schedulePlaceId) {
    return this.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`);
  }

  static async updateSchedulePlace(scheduleId, schedulePlaceId, placeData) {
    return this.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`, {
      method: 'PUT',
      body: JSON.stringify(placeData),
    });
  }

  static async removeSchedulePlace(scheduleId, schedulePlaceId) {
    return this.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`, {
      method: 'DELETE',
    });
  }

  // === 알림 ===
  static async getNotifications(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const endpoint = queryString
      ? `/notifications?${queryString}`
      : '/notifications';
    return this.request(endpoint);
  }

  static async markNotificationAsRead(notificationId) {
    return this.request(`/notifications/${notificationId}/read`, {
      method: 'PATCH',
    });
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
