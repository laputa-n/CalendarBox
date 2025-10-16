export const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

export const validateSchedule = (scheduleData) => {
  const errors = {};
  
  if (!scheduleData.title || scheduleData.title.trim().length === 0) {
    errors.title = '제목을 입력해주세요.';
  }
  
  if (!scheduleData.isAllDay) {
    if (!scheduleData.startDateTime) {
      errors.startDateTime = '시작 시간을 입력해주세요.';
    }
    
    if (!scheduleData.endDateTime) {
      errors.endDateTime = '종료 시간을 입력해주세요.';
    }
    
    if (scheduleData.startDateTime && scheduleData.endDateTime) {
      const start = new Date(scheduleData.startDateTime);
      const end = new Date(scheduleData.endDateTime);
      
      if (start >= end) {
        errors.endDateTime = '종료 시간은 시작 시간보다 늦어야 합니다.';
      }
    }
  }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
};

export const validateCalendar = (calendarData) => {
  const errors = {};
  
  if (!calendarData.name || calendarData.name.trim().length === 0) {
    errors.name = '캘린더 이름을 입력해주세요.';
  }
  
  if (calendarData.name && calendarData.name.length > 50) {
    errors.name = '캘린더 이름은 50자 이내로 입력해주세요.';
  }
  
  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
};