export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/kakao',
    LOGOUT: '/auth/logout',
    PROFILE: '/auth/profile',
    REFRESH: '/auth/refresh',
  },
  CALENDARS: {
    LIST: '/calendars',
    CREATE: '/calendars',
    UPDATE: (id) => `/calendars/${id}`,
    DELETE: (id) => `/calendars/${id}`,
    SHARE: (id) => `/calendars/${id}/share`,
  },
  SCHEDULES: {
    LIST: (calendarId) => `/calendars/${calendarId}/schedules`,
    CREATE: (calendarId) => `/calendars/${calendarId}/schedules`,
    UPDATE: (id) => `/schedules/${id}`,
    DELETE: (id) => `/schedules/${id}`,
    DETAIL: (id) => `/schedules/${id}`,
    SEARCH: '/schedules/search',
    INVITE: (id) => `/schedules/${id}/invite`,
  },
  FRIENDS: {
    LIST: '/friendships/received',
    REQUEST: '/friendships/request',
    ACCEPT: (id) => `/friendships/${id}`,
    REJECT: (id) => `/friendships/${id}`,
    REMOVE: (id) => `/friendships/${id}`,
  },
  NOTIFICATIONS: {
    LIST: '/notifications',
    READ: (id) => `/notifications/${id}/read`,
    READ_ALL: '/notifications/read-all',
    DELETE: (id) => `/notifications/${id}`,
  },
  STATISTICS: {
    OVERVIEW: '/statistics',
    ACTIVITY: '/statistics/activity',
  }
};

export const NOTIFICATION_TYPES = {
  FRIEND_REQUEST: 'friend_request',
  SCHEDULE_INVITE: 'schedule_invite',
  SCHEDULE_REMINDER: 'schedule_reminder',
  SCHEDULE_UPDATED: 'schedule_updated',
  CALENDAR_SHARED: 'calendar_shared',
};

export const SCHEDULE_STATUS = {
  CONFIRMED: 'confirmed',
  TENTATIVE: 'tentative',
  CANCELLED: 'cancelled',
};

export const FRIENDSHIP_STATUS = {
  PENDING: 'pending',
  ACCEPTED: 'accepted',
  REJECTED: 'rejected',
  BLOCKED: 'blocked',
};

export const CALENDAR_VISIBILITY = {
  PUBLIC: 'public',
  PRIVATE: 'private',
  SHARED: 'shared',
};
