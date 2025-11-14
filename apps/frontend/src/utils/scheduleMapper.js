// utils/scheduleMapper.js (새 파일로 두는 걸 추천)
export function toLocalISO(localStr) {
  if (!localStr) return null;
  return new Date(localStr).toISOString(); // "yyyy-MM-ddTHH:mm" -> ISO Z
}

// "5m","30m","1h","1d" 등을 minutesBefore로
export function mapReminders(remindersArr) {
  if (!Array.isArray(remindersArr)) return [];
  const toMinutes = (s) => {
    if (typeof s !== 'string') return null;
    if (s.endsWith('m')) return parseInt(s) || 0;
    if (s.endsWith('h')) return (parseInt(s) || 0) * 60;
    if (s.endsWith('d')) return (parseInt(s) || 0) * 60 * 24;
    return null;
  };
  return remindersArr
    .map(toMinutes)
    .filter((m) => Number.isFinite(m))
    .map((minutes) => ({ minutesBefore: minutes }));
}

// formData.todos = [{content,isDone, id?, ...}] or ["문자열",...]
export function mapTodos(todos) {
  if (!Array.isArray(todos)) return [];
  return todos.map((t, idx) => ({
    content: typeof t === 'string' ? t : (t.content ?? ''),
    isDone: typeof t === 'object' ? !!t.isDone : false,
    orderNo: idx + 1,
  }));
}

// formData.recurrence: 'DAILY'|'WEEKLY'|'MONTHLY' 또는 객체일 수 있음
// 백엔드 요구: { freq, intervalCount, byDay, until, exceptions }
export function mapRecurrence(recurrence) {
  if (!recurrence) return null;
  // 이미 객체로 들어오면 shape만 맞춰서 반환
  if (typeof recurrence === 'object') {
    const out = {
      freq: recurrence.freq ?? 'WEEKLY',
      intervalCount: recurrence.intervalCount ?? 1,
      byDay: recurrence.byDay ?? undefined,
      until: recurrence.until ?? undefined,
      exceptions: recurrence.exceptions ?? undefined,
    };
    return out;
  }
  // 문자열만 있는 간단 모드일 때
  const basic = String(recurrence).toUpperCase();
  return { freq: basic, intervalCount: 1 };
}

// formData.places -> 백엔드가 원하는 스키마로
export function mapPlaces(places) {
  if (!Array.isArray(places)) return [];
  return places.map((p) => ({
    mode: p.mode || 'PROVIDER',
    provider: p.provider || 'NAVER',
    providerPlaceKey: p.providerPlaceKey || '',
    title: p.title || p.name || '',
    category: p.category || '',
    address: p.address || '',
    roadAddress: p.roadAddress || '',
    link: p.link || '',
    lat: Number(p.lat),
    lng: Number(p.lng),
  }));
}

// participants는 현재 UI에 없으면 빈 배열
export function mapParticipants(participants) {
  if (!Array.isArray(participants)) return [];
  return participants.map((p) => ({
    mode: p.mode,            // "FRIEND" | "NAME" 등
    memberId: p.memberId,    // FRIEND일 때
    name: p.name,            // NAME일 때
  }));
}

// 링크도 현재 UI에 없으면 빈 배열
export function mapLinks(links) {
  if (!Array.isArray(links)) return [];
  return links.map((l) => ({ url: l.url, label: l.label }));
}

// 최종 페이로드 빌더
export function buildSchedulePayload(formData) {
  return {
    title: formData.title ?? '',
    memo: formData.description ?? '',
    theme: (formData.theme || formData.colorName || 'YELLOW').toUpperCase(),
    startAt: toLocalISO(formData.startDateTime),
    endAt: toLocalISO(formData.endDateTime),

    links: mapLinks(formData.links || []),
    todos: mapTodos(formData.todos || []),
    reminders: mapReminders(formData.reminders || []),
    recurrence: mapRecurrence(formData.recurrence), // null 가능

    participants: mapParticipants(formData.participants || []),
    places: mapPlaces(formData.places || []),
  };
}
