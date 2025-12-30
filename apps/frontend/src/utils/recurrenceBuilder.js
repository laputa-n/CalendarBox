// src/utils/recurrenceBuilder.js
import { localInputToISO } from './datetime';

export function buildRecurrencePayload(recurrence) {
  // ✅ 반복 없음
  if (!recurrence?.freq) return null;

  // ❌ 반복 있는데 until 없음 → 프론트에서 컷
  if (!recurrence.until || recurrence.until.trim() === '') {
    throw new Error('반복을 설정했다면 반복 종료일을 반드시 선택해야 합니다.');
  }

  return {
    freq: recurrence.freq,
    intervalCount: Number(recurrence.intervalCount) || 1,
    ...(recurrence.byDay?.length ? { byDay: recurrence.byDay } : {}),
    until: localInputToISO(recurrence.until), // ✅ 절대 빈 값 아님
  };
}
