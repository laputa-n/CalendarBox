export const formatDate = (date, options = {}) => {
  if (!date) return '';
  
  const dateObj = new Date(date);
  const defaultOptions = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    ...options
  };
  
  return new Intl.DateTimeFormat('ko-KR', defaultOptions).format(dateObj);
};

export const formatTime = (date, options = {}) => {
  if (!date) return '';
  
  const dateObj = new Date(date);
  const defaultOptions = {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    ...options
  };
  
  return new Intl.DateTimeFormat('ko-KR', defaultOptions).format(dateObj);
};

export const formatDateTime = (date) => {
  if (!date) return '';
  
  const dateStr = formatDate(date);
  const timeStr = formatTime(date);
  
  return `${dateStr} ${timeStr}`;
};

export const isToday = (date) => {
  if (!date) return false;
  const today = new Date();
  const targetDate = new Date(date);
  
  return today.toDateString() === targetDate.toDateString();
};

export const isSameDate = (date1, date2) => {
  if (!date1 || !date2) return false;
  
  const d1 = new Date(date1);
  const d2 = new Date(date2);
  
  return d1.toDateString() === d2.toDateString();
};

export const addDays = (date, days) => {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
};

export const getWeekDays = (startDate = new Date()) => {
  const start = new Date(startDate);
  start.setDate(start.getDate() - start.getDay());
  
  const days = [];
  for (let i = 0; i < 7; i++) {
    days.push(addDays(start, i));
  }
  
  return days;
};

export const getMonthDays = (year, month) => {
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const startDate = new Date(firstDay);
  startDate.setDate(startDate.getDate() - firstDay.getDay());
  
  const days = [];
  const endDate = new Date(lastDay);
  endDate.setDate(endDate.getDate() + (6 - lastDay.getDay()));
  
  let currentDate = new Date(startDate);
  while (currentDate <= endDate) {
    days.push(new Date(currentDate));
    currentDate.setDate(currentDate.getDate() + 1);
  }
  
  return days;
};