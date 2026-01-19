import React from 'react';
import CreateScheduleModal from './CreateScheduleModal';
import EditScheduleModal from './EditScheduleModal';
export default function ScheduleModal({ isOpen, onClose, selectedDate, eventData }) {
  if (!isOpen) return null;

  // ✅ 핵심: eventData에서 scheduleId를 최대한 뽑는다
  const scheduleId =
    eventData?.scheduleId ??
    eventData?.id ??
    eventData?.extendedProps?.scheduleId;

  console.log('🧩 [ScheduleModal] eventData =', eventData);
  console.log('🧩 [ScheduleModal] derived scheduleId =', scheduleId);

  // ✅ scheduleId가 있으면 무조건 수정 모드
  if (scheduleId) {
    return (
      <EditScheduleModal
        isOpen={isOpen}
        onClose={onClose}
        eventData={eventData}
        scheduleId={scheduleId}
      />
    );
  }

  // ✅ eventData는 있는데 scheduleId가 없으면 여기서 바로 잡힘
  if (eventData && !scheduleId) {
    return (
      <div style={{ padding: 16, background: '#fff' }}>
        <p style={{ color: '#ef4444' }}>
          ⚠️ eventData는 있으나 scheduleId를 찾지 못했습니다. 콘솔 로그를 확인하세요.
        </p>
        <button onClick={onClose}>닫기</button>
      </div>
    );
  }

  // ✅ 생성 모드
  return (
    <CreateScheduleModal
      isOpen={isOpen}
      onClose={onClose}
      selectedDate={selectedDate}
    />
  );
}
