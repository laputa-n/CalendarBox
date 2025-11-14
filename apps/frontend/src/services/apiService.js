// src/services/apiService.js

const API_CONFIG = {
  development: 'http://localhost:8080/api',
  staging: '/api',
  production: '/api',
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

  // === 통계 ===
  static async getStatistics(period = 'month') {
    return this.request(`/statistics?period=${period}`);
  }

  static async getUserActivity() {
    return this.request('/statistics/activity');
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

static async createReminder(scheduleId, minutesBefore) {
  return this.request(`/schedules/${scheduleId}/reminders`, {
    method: 'POST',
    body: JSON.stringify({ minutesBefore }),
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
  console.log('recurrenceData:', recurrenceData); // 값 확인
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
  const response = await this.request(`/schedules/${scheduleId}/recurrences/${recurrenceId}`, {
    method: 'PUT',
    body: JSON.stringify(recurrenceData),
  });
  return response;
}

static async deleteRecurrence(scheduleId, recurrenceId) {
  return this.request(`/schedules/${scheduleId}/recurrences/${recurrenceId}`, { method: 'DELETE' });
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


