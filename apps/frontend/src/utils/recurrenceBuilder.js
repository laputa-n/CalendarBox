// src/utils/recurrenceBuilder.js
import { localInputToISO } from './datetime';

export function buildRecurrencePayload(recurrence) {
  // ✅ 반복 없음
  if (!recurrence?.freq) return null;

  // ✅ 반복 있는데 until 없음 → 프론트에서 컷
  if (!recurrence.until || String(recurrence.until).trim() === '') {
    throw new Error('반복을 설정했다면 반복 종료일을 반드시 선택해야 합니다.');
  }

  const freq = recurrence.freq;
  const intervalCount = Number(recurrence.intervalCount) || 1;

  const byDay = Array.isArray(recurrence.byDay) ? recurrence.byDay.filter(Boolean) : [];

  // ✅ UI state: byMonthDay (number | ''),  스펙: byMonthday (int[])
  const raw = recurrence.byMonthDay;
  const d = raw === '' || raw == null ? null : Number(raw);
  const byMonthday = Number.isInteger(d) && d >= 1 && d <= 31 ? [d] : [];

  // ✅ 스펙에 byMonth도 존재 (일단 빈 배열로 고정)
  const byMonth = [];

  // ✅ until은 스펙 예시처럼 date-time(Z)로 보내도 됨
  const until = localInputToISO(recurrence.until);

  // ✅ MONTHLY인데 byDay도 byMonthday도 없으면 프론트에서 차단
  if (freq === 'MONTHLY' && byDay.length === 0 && byMonthday.length === 0) {
    throw new Error('MONTHLY 반복은 ByDay 또는 ByMonthday(1~31) 중 하나가 필요합니다.');
  }

  // ✅ WEEKLY인데 요일 없으면 차단(선택)
  if (freq === 'WEEKLY' && byDay.length === 0) {
    throw new Error('WEEKLY 반복은 요일을 1개 이상 선택해야 합니다.');
  }

  return {
    freq,
    intervalCount,
    byDay,
    byMonthday, // ✅ 스펙 키 + 배열
    byMonth,    // ✅ 스펙 키
    until,
  };
}
