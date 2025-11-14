// src/utils/datetime.js
export const toLocalInputValue = (isoOrLocal) => {
  if (!isoOrLocal) return '';
  const d = new Date(isoOrLocal);
  if (Number.isNaN(d.getTime())) return isoOrLocal; // 이미 로컬 포맷일 수 있음
  const pad = (n) => String(n).padStart(2, '0');
  const yyyy = d.getFullYear();
  const mm = pad(d.getMonth() + 1);
  const dd = pad(d.getDate());
  const hh = pad(d.getHours());
  const mi = pad(d.getMinutes());
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}`;
};

export const localInputToISO = (localStr) => {
  // "YYYY-MM-DDThh:mm" → 로컬타임 기준 ISO
  if (!localStr) return null;
  const d = new Date(localStr);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
};
