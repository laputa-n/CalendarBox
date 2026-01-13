import React from 'react';
import CreateScheduleModal from './CreateScheduleModal';
import EditScheduleModal from './EditScheduleModal';

export default function ScheduleModal({ isOpen, onClose, selectedDate, eventData }) {
  if (!isOpen) return null;

  // ✅ eventData 있으면 수정 모드, 없으면 생성 모드
  if (eventData) {
    console.log('🧩 수정 모드 eventData:', JSON.stringify(eventData));
    return (
      <EditScheduleModal
        isOpen={isOpen}
        onClose={onClose}
        eventData={eventData}   // ✅ 핵심
      />
    );
  }

  console.log('🧩 [ScheduleModal] 생성 모드');
  return (
    <CreateScheduleModal
      isOpen={isOpen}
      onClose={onClose}
      selectedDate={selectedDate}
    />
  );
}
