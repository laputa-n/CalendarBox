// src/services/apiService.js
const API_CONFIG = {
  development: 'http://localhost:8080/api', // 로컬에서만 localhost 사용
  production: '/api',                       // 서버에서는 같은 호스트 + /api 로만
  staging: '/api',                          // 있으면 같이 맞춰도 됨

   // development: 'http://localhost:8080/api',
   // staging: 'https://api-staging.calbox.com/api',
   // production: 'https://api.calbox.com/api',
};
const API_BASE_URL = API_CONFIG[process.env.NODE_ENV] || API_CONFIG.development;
const getAuthToken = () => localStorage.getItem('accessToken');
export class ApiService {
  /**
   * 공통 fetch 래퍼
   */
  static async request(endpoint, options = {}) {
    const { headers = {}, ...rest } = options;
    const hasBody = rest.body !== undefined && rest.body !== null;
    const base = API_BASE_URL.endsWith('/') ? API_BASE_URL : `${API_BASE_URL}/`;
 const path = endpoint.startsWith('/') ? endpoint.slice(1) : endpoint;
 const fullUrl = base + path;

    const config = {
      credentials: 'include', // ✅ 항상 쿠키 포함
      ...rest,
      headers: {
        ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
        ...headers,
      },
    };

   const res = await fetch(fullUrl, config);

    if (!res.ok) {
      const ct = res.headers.get('content-type') || '';
      const errorData = ct.includes('application/json')
        ? await res.json().catch(() => ({}))
        : {};
      const err = new Error(errorData?.message || `HTTP ${res.status}`);
   err.status = res.status;
   err.url = fullUrl;
   err.data = errorData;
  console.error('[API ERROR]', {
     url: fullUrl,
     status: res.status,
     serverMessage: errorData?.message,
     serverBody: errorData,
  });

   throw err;
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

  // === 회원 검색 기능 관련 ===
static async searchMembers(q, page = 0, size = 20, sort = null) {
  let endpoint = `/members/search?q=${encodeURIComponent(q)}&page=${page}&size=${size}`;

  if (sort && Array.isArray(sort)) {
    sort.forEach(s => {
      endpoint += `&sort=${encodeURIComponent(s)}`;
    });
  }

  return this.request(endpoint);
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
  return this.request(`/calendars/${calendarId}/members`, {
    method: 'POST',
    body: JSON.stringify({ members: memberIds }), // ✅ 명세 핵심
  });
}

static async getCalendarMembers(calendarId, params = {}) {
  const qs = new URLSearchParams();

  if (params.status) qs.append('status', params.status);
  if (params.sort) qs.append('sort', params.sort);
  if (params.page !== undefined) qs.append('page', params.page);
  if (params.size !== undefined) qs.append('size', params.size);

  const query = qs.toString();
  return this.request(`/calendars/${calendarId}/members${query ? `?${query}` : ''}`, {
    method: 'GET',
  });
}

static async respondToCalendarInvite(calendarMemberId, action) {
  return this.request(`/calendar-members/${calendarMemberId}`, {
    method: 'PATCH',
    body: JSON.stringify({ action }), // ACCEPT / REJECT
  });
}

static async removeCalendarMember(calendarMemberId) {
  const response = await this.request(`/calendar-members/${calendarMemberId}`, {
    method: 'DELETE',
  });
  return response;
}

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

// ✅ 일정 상세 조회
static async getScheduleDetail(scheduleId) {
  return this.request(`/schedules/${scheduleId}`);
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
      method: 'PATCH',
      body: JSON.stringify({ action }), // 🔥 필수
    }
  );
}

  // 🔍 일정 검색
static async searchSchedules({ query, calendarId }) {
  const params = new URLSearchParams();

  if (query) params.append('query', query);

  if (calendarId) {
    // 여러 개 가능
    if (Array.isArray(calendarId)) {
      calendarId.forEach(id => params.append('calendarId', id));
    } else {
      params.append('calendarId', calendarId);
    }
  }

  return this.request(`/schedules/search?${params.toString()}`, {
    method: 'GET',
  });
}

// 📋 일정 복제
static async cloneSchedule(calendarId, sourceScheduleId, targetDate) {
  return this.request(`/calendars/${calendarId}/schedules`, {
    method: 'POST',
    body: JSON.stringify({
      sourceScheduleId,
      targetDate,
    }),
  });
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

  // === 일정 장소 순서 재정렬 ===
  static async reorderSchedulePlaces(scheduleId, positions) {
    return this.request(`/schedules/${scheduleId}/places`, {
      method: 'PATCH',
      body: JSON.stringify({ positions }),
    });
  }

  // === 일정 장소 상세 조회 ===
  static async getSchedulePlaceById(scheduleId, schedulePlaceId) {
    return this.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`);
  }

  // === 일정 장소 이름 수정 ===
  static async updateSchedulePlace(scheduleId, schedulePlaceId, name) {
    return this.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`, {
      method: 'PATCH',
      body: JSON.stringify({ name }),
    });
  }

  // === 일정 장소 삭제 ===
  static async removeSchedulePlace(scheduleId, schedulePlaceId) {
    return this.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`, {
      method: 'DELETE',
    });
  }

  // === 첨부파일 관련 ===
static async getPresignedUrl(scheduleId, file, isReceipt = false) {
  console.log('[DEBUG] getPresignedUrl called with:', {
    scheduleId,
    filename: file.name,
    type: file.type,
    isReceipt,
  });

  return this.request(`/attachments/uploads/presign`, {
    method: 'POST',
    body: JSON.stringify({
      scheduleId: Number(scheduleId),
      filename: file.name,
      contentType: file.type,
      size: file.size,
      isReceipt: Boolean(isReceipt),
    }),
  });
}

static async completeUpload(uploadId, objectKey, isReceipt = false) {
  return this.request(`/attachments/uploads/complete`, {
    method: 'POST',
    body: JSON.stringify({
      uploadId,
      objectKey,
      isReceipt,
    }),
  });
}

static async getImageAttachments(scheduleId) {
  return this.request(`/schedules/${scheduleId}/attachments/images`);
}

static async getFileAttachments(scheduleId) {
  return this.request(`/schedules/${scheduleId}/attachments/files`);
}

static async deleteAttachment(attachmentId) {
  return this.request(`/attachments/${attachmentId}`, { method: 'DELETE' });
}

static async getDownloadUrl(attachmentId) {
  return this.request(`/attachments/${attachmentId}/download`);
}
// ✅ 일정 투두 관련 API
static async getTodos(scheduleId) {
  return this.request(`/schedules/${scheduleId}/todos`, { method: 'GET' });
}

static async addTodo(scheduleId, content) {
  return this.request(`/schedules/${scheduleId}/todos`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  });
}

static async updateTodo(scheduleId, todoId, content) {
  return this.request(`/schedules/${scheduleId}/todos/${todoId}`, {
    method: 'PATCH',
    body: JSON.stringify({ content }),
  });
}

static async toggleTodo(scheduleId, todoId) {
  return this.request(`/schedules/${scheduleId}/todos/${todoId}/toggle`, {
    method: 'PATCH',
  });
}

static async reorderTodos(scheduleId, orders) {
  return this.request(`/schedules/${scheduleId}/todos/reorder`, {
    method: 'PATCH',
    body: JSON.stringify({ orders }),
  });
}

static async deleteTodo(scheduleId, todoId) {
  return this.request(`/schedules/${scheduleId}/todos/${todoId}`, {
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

  // === 일정 지출 관련 ===
  static async listExpenses(scheduleId, page = 0, size = 50) {
    return this.request(`/schedules/${scheduleId}/expenses?page=${page}&size=${size}`, {
      method: 'GET',
    });
  }

  static async createExpense(scheduleId, expenseData) {
    return this.request(`/schedules/${scheduleId}/expenses`, {
      method: 'POST',
      body: JSON.stringify(expenseData),
    });
  }

  static async getExpenseDetail(scheduleId, expenseId) {
    return this.request(`/schedules/${scheduleId}/expenses/${expenseId}`, {
      method: 'GET',
    });
  }

  static async updateExpense(scheduleId, expenseId, partialData) {
    return this.request(`/schedules/${scheduleId}/expenses/${expenseId}`, {
      method: 'PATCH',
      body: JSON.stringify(partialData),
    });
  }

  static async deleteExpense(scheduleId, expenseId) {
    return this.request(`/schedules/${scheduleId}/expenses/${expenseId}`, {
      method: 'DELETE',
    });
  }

  // === 일정 지출 상세 항목(Expense Lines) ===
  static async listExpenseLines(expenseId, page = 0, size = 100) {
    return this.request(`/expenses/${expenseId}/lines?page=${page}&size=${size}`, {
      method: 'GET',
    });
  }

  static async createExpenseLine(expenseId, lineData) {
    return this.request(`/expenses/${expenseId}/lines`, {
      method: 'POST',
      body: JSON.stringify(lineData),
    });
  }

  static async updateExpenseLine(expenseId, lineId, partialData) {
    return this.request(`/expenses/${expenseId}/lines/${lineId}`, {
      method: 'PATCH',
      body: JSON.stringify(partialData),
    });
  }

  static async deleteExpenseLine(expenseId, lineId) {
    return this.request(`/expenses/${expenseId}/lines/${lineId}`, {
      method: 'DELETE',
    });
  }

// === 일정 리마인더 관련 API ===

static async createReminder(scheduleId, minutes) {
  return this.request(`/schedules/${scheduleId}/reminders`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ minutesBefore: minutes }),
  });
}

static async listReminders(scheduleId) {
  return this.request(`/schedules/${scheduleId}/reminders`, {
    method: 'GET',
  });
}
static async deleteReminder(scheduleId, reminderId) {
  return this.request(`/schedules/${scheduleId}/reminders/${reminderId}`, {
    method: 'DELETE',
  });
}

static async createScheduleLink(scheduleId, { url, label }) {
  const response = await this.request(`/schedules/${scheduleId}/links`, {
    method: 'POST',
    body: JSON.stringify({ url, label }),
  });
  return response;
}

static async getScheduleLinks(scheduleId) {
  return this.request(`/schedules/${scheduleId}/links`, 
    { method: 'GET' });
}

static async deleteScheduleLink(scheduleId, linkId) {
  return this.request(`/schedules/${scheduleId}/links/${linkId}`, 
    { method: 'DELETE' });
}
//===========반복 ==============
static async createRecurrence(scheduleId, recurrenceData) {
  const response = await this.request(`/schedules/${scheduleId}/recurrences`, {
    method: 'POST',
    body: JSON.stringify(recurrenceData),
  });
  return response;
}

static async getRecurrences(scheduleId) {
  return this.request(`/schedules/${scheduleId}/recurrences`, { method: 'GET' });
}

static async updateRecurrence(scheduleId, recurrenceId, recurrenceData) {
  const response = await this.request(
    `/schedules/${scheduleId}/recurrences/${recurrenceId}`,
    {
      method: "PUT",
      body: JSON.stringify(recurrenceData),
    }
  );

  console.log("📡 [API 응답 - updateRecurrence]:", response);
  return response;
}

static async deleteRecurrence(scheduleId, recurrenceId) {
  const response = await this.request(
    `/schedules/${scheduleId}/recurrences/${recurrenceId}`,
    { method: 'DELETE' }
  );
  return response;
}

// 🔍 1) 반복 예외 목록 조회
static async getRecurrenceExceptions(scheduleId, recurrenceId) {
  return this.request(
    `/schedules/${scheduleId}/recurrences/${recurrenceId}/exceptions`,
    { method: 'GET' }
  );
}
// ➕ 2) 반복 예외 생성
static async createRecurrenceException(scheduleId, recurrenceId, dateString) {
  const payload = { exceptionDate: dateString };
  return this.request(
    `/schedules/${scheduleId}/recurrences/${recurrenceId}/exceptions`,
    {
      method: 'POST',
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    }
  );
}
// 🗑 3) 반복 예외 삭제
static async deleteRecurrenceException(scheduleId, recurrenceId, exceptionId) {
  return this.request(
    `/schedules/${scheduleId}/recurrences/${recurrenceId}/exceptions/${exceptionId}`,
    { method: 'DELETE' }
  );
}
static async getAllOccurrences({ fromKst, toKst }) {
  const response = await this.request(
    `/occurrences?from=${encodeURIComponent(fromKst)}&to=${encodeURIComponent(toKst)}`,
    { method: "GET" }
  );

  console.log("📡 [API 응답 - 전체 오커런스]:", response);
  return response;
}
static async getCalendarOccurrences(calendarId, { fromKst, toKst }) {
  const response = await this.request(
    `/calendars/${calendarId}/occurrences?from=${encodeURIComponent(fromKst)}&to=${encodeURIComponent(toKst)}`,
    { method: "GET" }
  );

  console.log(`📡 [API 응답 - 캘린더(${calendarId}) 오커런스]:`, response);
  return response;

  
}
 // === 사람(일정, 지출) 통계 요약 및 top3 ===
  static async getPeopleSummary(yearMonth) {
    return this.request(`/analytics/people/summary?yearMonth=${yearMonth}`, { method: 'GET' });
}
  // === 사람(일정, 지출) 통계 목록 조회 ===
 static async getPeopleList(yearMonth, page = 1, size = 10) {
    return this.request(`/analytics/people?yearMonth=${yearMonth}&page=${page - 1}&size=${size}`, { method: 'GET' });
}

  // === 장소(일정, 지출) 통계 요약 및 top3 ===
 static async getPlaceSummary(yearMonth) {
    return this.request(`/analytics/place/summary?yearMonth=${yearMonth}`, { method: 'GET' });
}
  // === 장소(일정, 지출) 통계 목록 조회 ===
  static async getPlaceList(yearMonth, page = 1, size = 10) {
    return this.request(`/analytics/place?yearMonth=${yearMonth}&page=${page - 1}&size=${size}`, { method: 'GET' });
}

  // === 요일-시간대 별 스케줄 분포 조회 ===
  static async getScheduleDayHourDistribution() {
    return this.request('/analytics/schedule/day-hour', { method: 'GET' });
  }

  // === 월별 스케줄 추이 조회 ===
  static async getMonthlyScheduleTrend() {
    return this.request('/analytics/schedule/trend', { method: 'GET' });
  }

}
// ✅ 클래스 바깥(닫는 } 다음 줄)에 붙여야 함
ApiService.getScheduleSummary = (scheduleId) =>
  ApiService.request(`/schedules/${scheduleId}`, { method: 'GET' });

ApiService.listSchedulePlaces = (scheduleId, page = 0, size = 20) =>
  ApiService.request(`/schedules/${scheduleId}/places?page=${page}&size=${size}`, { method: 'GET' });

ApiService.getSchedulePlaceDetail = (scheduleId, schedulePlaceId) =>
  ApiService.request(`/schedules/${scheduleId}/places/${schedulePlaceId}`, { method: 'GET' });

ApiService.listTodos = (scheduleId, page = 0, size = 50) =>
  ApiService.request(`/schedules/${scheduleId}/todos?page=${page}&size=${size}`, { method: 'GET' });

ApiService.listImageAttachments = (scheduleId, page = 0, size = 20) =>
  ApiService.request(`/schedules/${scheduleId}/attachments/images?page=${page}&size=${size}`, { method: 'GET' });

ApiService.listFileAttachments = (scheduleId, page = 0, size = 20) =>
  ApiService.request(`/schedules/${scheduleId}/attachments/files?page=${page}&size=${size}`, { method: 'GET' });
