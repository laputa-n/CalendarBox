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
  // 👉 /auth/me가 쿠키 검사 후 { member: {...} } 리턴
  return this.request('/auth/me', { method: 'GET' });
}

static async completeSignup(signupData) {
  return this.request('/auth/signup/complete', {
    method: 'POST',
    body: JSON.stringify(signupData),
  });
}

// ✅ 신규 회원 이메일 확인용 API
static async getNextAction() {
  return this.request('/auth/kakao/next', {
    method: 'GET',
    credentials: 'include', // 👉 쿠키 항상 포함
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
    if (status) {
      endpoint += `&status=${status}`;
    }
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
    method: 'PATCH',  // PUT → PATCH
    body: JSON.stringify({ action: 'ACCEPT' })  // body 추가
  });
}

static async rejectFriendRequest(friendshipId) {
  return this.request(`/friendships/${friendshipId}`, {
    method: 'PATCH',  // PUT → PATCH
    body: JSON.stringify({ action: 'REJECT' })  // body 추가
  });
}
  // === 캘린더 관련 API ===
  static async getCalendars(page = 1, size = 20) {
    const response = await this.request(`/calendars?page=${page - 1}&size=${size}`);
    return this.handleApiResponse(response);
  }

  static async createCalendar(calendarData) {
    const requestData = {
      name: calendarData.name,
      type: calendarData.type || 'PERSONAL',
      visibility: calendarData.visibility || 'PRIVATE'
    };

    const response = await this.request('/calendars', {
      method: 'POST',
      body: JSON.stringify(requestData),
    });
    return this.handleApiResponse(response);
  }

  static async getCalendarById(calendarId) {
    const response = await this.request(`/calendars/${calendarId}`);
    return this.handleApiResponse(response);
  }

  static async updateCalendar(calendarId, calendarData) {
    const requestData = {
      name: calendarData.name,
      visibility: calendarData.visibility
    };

    const response = await this.request(`/calendars/${calendarId}`, {
      method: 'PUT',
      body: JSON.stringify(requestData),
    });
    return this.handleApiResponse(response);
  }

  static async deleteCalendar(calendarId) {
    const response = await this.request(`/calendars/${calendarId}`, { 
      method: 'DELETE' 
    });
    return this.handleApiResponse(response);
  }

  // === 캘린더 멤버 관련 API ===
  static async inviteCalendarMembers(calendarId, memberIds) {
    const response = await this.request(`/calendars/${calendarId}/members`, {
      method: 'POST',
      body: JSON.stringify({ memberIds }),
    });
    return this.handleApiResponse(response);
  }

  static async getCalendarMembers(calendarId, page = 1, size = 10) {
    const response = await this.request(`/calendars/${calendarId}/members?page=${page}&size=${size}`);
    return this.handleApiResponse(response);
  }

  static async respondToCalendarInvite(calendarMemberId, status) {
    const response = await this.request(`/calendar-members/${calendarMemberId}`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    });
    return this.handleApiResponse(response);
  }

  static async removeCalendarMember(calendarMemberId) {
    const response = await this.request(`/calendar-members/${calendarMemberId}`, { 
      method: 'DELETE' 
    });
    return this.handleApiResponse(response);
  }

  // === 일정 관련 API (API 명세에 맞춰 수정) ===

  // 특정 캘린더의 일정 목록 조회
  static async getCalendarSchedules(calendarId, params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const endpoint = queryString
      ? `/calendars/${calendarId}/schedules?${queryString}`
      : `/calendars/${calendarId}/schedules`;
    const response = await this.request(endpoint);
    return this.handleApiResponse(response);
  }

  // 전체 일정 목록 조회 (API 명세: /schedules)
  static async getSchedules(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const endpoint = queryString ? `/schedules?${queryString}` : '/schedules';
    const response = await this.request(endpoint);
    return this.handleApiResponse(response);
  }

  // 일정 생성 (API 명세: POST /calendars/{calendarId}/schedules)
  static async createSchedule(calendarId, scheduleData) {
    const response = await this.request(`/calendars/${calendarId}/schedules`, {
      method: 'POST',
      body: JSON.stringify(scheduleData),
    });
    return this.handleApiResponse(response);
  }

  // 일정 상세 조회 (API 명세: GET /schedules/{scheduleId})
  static async getScheduleById(scheduleId) {
    const response = await this.request(`/schedules/${scheduleId}`);
    return this.handleApiResponse(response);
  }

  // 일정 수정 (API 명세: PUT /schedules/{scheduleId})
  static async updateSchedule(scheduleId, scheduleData) {
    const response = await this.request(`/schedules/${scheduleId}`, {
      method: 'PUT',
      body: JSON.stringify(scheduleData),
    });
    return this.handleApiResponse(response);
  }

  // 일정 삭제 (API 명세: DELETE /schedules/{scheduleId})
  static async deleteSchedule(scheduleId) {
    const response = await this.request(`/schedules/${scheduleId}`, { 
      method: 'DELETE' 
    });
    return this.handleApiResponse(response);
  }

  // 일정 검색 (API 명세: GET /schedules/search)
  static async searchSchedules(query, filters = {}) {
    const params = new URLSearchParams({ query, ...filters });
    const response = await this.request(`/schedules/search?${params}`);
    return this.handleApiResponse(response);
  }

  // 일정 복제 (API 명세: POST /calendars/{calendarId}/schedules - 복제용)
  static async duplicateSchedule(calendarId, sourceScheduleId) {
    const response = await this.request(`/calendars/${calendarId}/schedules`, {
      method: 'POST',
      body: JSON.stringify({ sourceScheduleId }),
    });
    return this.handleApiResponse(response);
  }

  // === 일정 참여자 관련 API (API 명세에 맞춰 새로 추가) ===

  // 일정 멤버 등록 (API 명세: POST /schedules/{scheduleId}/participants)
  static async addScheduleParticipant(scheduleId, participantData) {
    const response = await this.request(`/schedules/${scheduleId}/participants`, {
      method: 'POST',
      body: JSON.stringify(participantData),
    });
    return this.handleApiResponse(response);
  }

  // 일정 멤버 목록 조회 (API 명세: GET /schedules/{scheduleId}/participants)
  static async getScheduleParticipants(scheduleId) {
    const response = await this.request(`/schedules/${scheduleId}/participants`);
    return this.handleApiResponse(response);
  }

  // 일정 멤버 삭제 (API 명세: DELETE /schedules/{scheduleId}/participants/{participantId})
  static async removeScheduleParticipant(scheduleId, participantId) {
    const response = await this.request(`/schedules/${scheduleId}/participants/${participantId}`, {
      method: 'DELETE',
    });
    return this.handleApiResponse(response);
  }

  // 일정 멤버 응답 (API 명세: PUT /schedules/{scheduleId}/participants/{participantId})
  static async respondToScheduleInvite(scheduleId, participantId, action) {
    const response = await this.request(`/schedules/${scheduleId}/participants/${participantId}`, {
      method: 'PUT',
      body: JSON.stringify({ action }), // ACCEPT or REJECT
    });
    return this.handleApiResponse(response);
  }

  // === 장소 관련 API (API 명세에 맞춰 새로 추가) ===

  // 장소 검색 (API 명세: GET /places/search)
  static async searchPlaces(query) {
    const response = await this.request(`/places/search?query=${encodeURIComponent(query)}`);
    return this.handleApiResponse(response);
  }

  // 일정 장소 등록 (API 명세: POST /schedules/{scheduleId}/places)
  static async addSchedulePlace(scheduleId, placeData) {
    const response = await this.request(`/schedules/${scheduleId}/places`, {
      method: 'POST',
      body: JSON.stringify(placeData),
    });
    return this.handleApiResponse(response);
  }

  // 일정 장소 목록 조회
  static async getSchedulePlaces(scheduleId) {
    const response = await this.request(`/schedules/${scheduleId}/places`);
    return this.handleApiResponse(response);
  }

  // 일정 장소 순서 재정렬 (API 명세: PUT /schedules/{scheduleId}/places)
  static async reorderSchedulePlaces(scheduleId, placesOrder) {
    const response = await this.request(`/schedules/${scheduleId}/places`, {
      method: 'PUT',
      body: JSON.stringify(placesOrder),
    });
    return this.handleApiResponse(response);
  }

  // 일정 장소 상세 조회 (API 명세: GET /schedules/{scheduleId}/places/{schedulePlaceId})
  static async getSchedulePlaceById(scheduleId, schedulePlaceId) {
    const response = await this.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`);
    return this.handleApiResponse(response);
  }

  // 일정 장소 수정 (API 명세: PUT /schedules/{scheduleId}/places/{schedulePlaceId})
  static async updateSchedulePlace(scheduleId, schedulePlaceId, placeData) {
    const response = await this.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`, {
      method: 'PUT',
      body: JSON.stringify(placeData),
    });
    return this.handleApiResponse(response);
  }

  // 일정 장소 삭제 (API 명세: DELETE /schedules/{scheduleId}/places/{schedulePlaceId})
  static async removeSchedulePlace(scheduleId, schedulePlaceId) {
    const response = await this.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`, {
      method: 'DELETE',
    });
    return this.handleApiResponse(response);
  }

  // === 기존 API들 (일단 유지) ===
  static async inviteToSchedule(scheduleId, inviteData) {
    const response = await this.request(`/schedules/${scheduleId}/invite`, {
      method: 'POST',
      body: JSON.stringify(inviteData),
    });
    return this.handleApiResponse(response);
  }

  static async respondToInvitation(invitationId, response) {
    const res = await this.request(`/invitations/${invitationId}/respond`, {
      method: 'PATCH',
      body: JSON.stringify({ response }),
    });
    return this.handleApiResponse(res);
  }

  // === 알림 ===
  static async getNotifications(params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const endpoint = queryString ? `/notifications?${queryString}` : '/notifications';
    const response = await this.request(endpoint);
    return this.handleApiResponse(response);
  }

  static async markNotificationAsRead(notificationId) {
    const response = await this.request(`/notifications/${notificationId}/read`, { 
      method: 'PATCH' 
    });
    return this.handleApiResponse(response);
  }

  static async markAllNotificationsAsRead() {
    const response = await this.request('/notifications/read-all', { 
      method: 'PATCH' 
    });
    return this.handleApiResponse(response);
  }

  static async deleteNotification(notificationId) {
    const response = await this.request(`/notifications/${notificationId}`, { 
      method: 'DELETE' 
    });
    return this.handleApiResponse(response);
  }

  // === 통계 ===
  static async getStatistics(period = 'month') {
    const response = await this.request(`/statistics?period=${period}`);
    return this.handleApiResponse(response);
  }

  static async getUserActivity() {
    const response = await this.request('/statistics/activity');
    return this.handleApiResponse(response);
  }
}