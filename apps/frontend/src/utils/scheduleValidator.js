// utils/scheduleValidator.js
export function validateSchedulePayload(p) {
  const errs = [];
  if (!p.title) errs.push('title 누락');
  if (!p.startAt) errs.push('startAt 누락/형식 오류');
  if (!p.endAt) errs.push('endAt 누락/형식 오류');

  if (p.recurrence) {
    const okFreq = ['DAILY','WEEKLY','MONTHLY','YEARLY'];
    if (!okFreq.includes(p.recurrence.freq)) errs.push('recurrence.freq 잘못됨');
    if (p.recurrence.byDay && !Array.isArray(p.recurrence.byDay)) errs.push('recurrence.byDay 형식');
  }

  p.places?.forEach((pl, i) => {
    if (pl.mode === 'PROVIDER' && !pl.providerPlaceKey) errs.push(`places[${i}] providerPlaceKey 누락`);
  });

  return errs;
}
