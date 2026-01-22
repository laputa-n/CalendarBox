// src/components/schedule/RecurrenceViewModal.js
import React, { useMemo } from "react";

const overlayStyle = {
  position: "fixed",
  top: "0",
  left: "0",
  width: "100%",
  height: "100%",
  backgroundColor: "rgba(0,0,0,0.5)",
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  zIndex: 1200,
};

const modalStyle = {
  backgroundColor: "#fff",
  padding: "1.25rem",
  borderRadius: "12px",
  width: "520px",
  maxWidth: "92%",
  maxHeight: "85vh",
  overflowY: "auto",
};

const sectionStyle = { marginBottom: "1rem" };
const labelStyle = { fontWeight: 700, fontSize: "0.95rem", marginBottom: 6 };
const subText = { fontSize: "0.85rem", color: "#6b7280" };

const buttonRow = {
  display: "flex",
  justifyContent: "space-between",
  gap: 8,
  marginTop: 12,
};

const btn = {
  padding: "0.55rem 0.9rem",
  borderRadius: 10,
  border: "none",
  cursor: "pointer",
  background: "#e5e7eb",
};

const primaryBtn = { ...btn, background: "#2563eb", color: "#fff" };

const WEEKDAY_MAP = { SU: 0, MO: 1, TU: 2, WE: 3, TH: 4, FR: 5, SA: 6 };
const WEEKDAY_LABEL = { MO: "월", TU: "화", WE: "수", TH: "목", FR: "금", SA: "토", SU: "일" };

function ymd(d) {
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

function parseUntilISO(untilISO) {
  if (!untilISO) return null;
  const d = new Date(untilISO);
  return Number.isNaN(d.getTime()) ? null : d;
}

function parseByDayOrdinal(rule) {
  const m = String(rule || "").match(/^(-?\d+)(MO|TU|WE|TH|FR|SA|SU)$/);
  if (!m) return null;
  return { ord: Number(m[1]), day: m[2] };
}

function nthWeekdayOfMonth(year, monthIndex0, weekday0Sun, ord) {
  const first = new Date(year, monthIndex0, 1);
  const last = new Date(year, monthIndex0 + 1, 0);

  if (ord > 0) {
    const firstWeekday = first.getDay();
    const delta = (weekday0Sun - firstWeekday + 7) % 7;
    const day = 1 + delta + (ord - 1) * 7;
    const candidate = new Date(year, monthIndex0, day);
    if (candidate.getMonth() !== monthIndex0) return null;
    return candidate;
  } else {
    const lastWeekday = last.getDay();
    const delta = (lastWeekday - weekday0Sun + 7) % 7;
    const day = last.getDate() - delta + (ord + 1) * 7;
    const candidate = new Date(year, monthIndex0, day);
    if (candidate.getMonth() !== monthIndex0) return null;
    return candidate;
  }
}

function addDays(date, days) {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

function buildSummary(r) {
  if (!r?.freq) return "반복 없음";

  const interval = Number(r.intervalCount) || 1;
  const untilText = r.until ? ` ~ ${r.until}` : "";

  if (r.freq === "DAILY") {
    return interval === 1 ? `매일${untilText}` : `${interval}일마다${untilText}`;
  }

  if (r.freq === "WEEKLY") {
    const days = Array.isArray(r.byDay) ? r.byDay : [];
    const label = days.length ? days.map(d => WEEKDAY_LABEL[d] || d).join(", ") : "요일 미설정";
    return interval === 1 ? `매주 (${label})${untilText}` : `${interval}주마다 (${label})${untilText}`;
  }

  if (r.freq === "MONTHLY") {
    const byDay = Array.isArray(r.byDay) ? r.byDay : [];
    const byMonthday = Array.isArray(r.byMonthday) ? r.byMonthday : [];

    if (byDay.length) {
      const p = parseByDayOrdinal(byDay[0]);
      if (p) {
        const ordLabel =
          p.ord === 1 ? "첫째" :
          p.ord === 2 ? "둘째" :
          p.ord === 3 ? "셋째" :
          p.ord === 4 ? "넷째" :
          p.ord === 5 ? "다섯째" :
          p.ord === -1 ? "마지막" :
          p.ord === -2 ? "끝에서 둘째" :
          p.ord === -3 ? "끝에서 셋째" :
          p.ord === -4 ? "끝에서 넷째" :
          "주차";
        return interval === 1
          ? `매월 ${ordLabel} ${WEEKDAY_LABEL[p.day] || p.day}요일${untilText}`
          : `${interval}개월마다 ${ordLabel} ${WEEKDAY_LABEL[p.day] || p.day}요일${untilText}`;
      }
      return `매월 (ByDay: ${byDay.join(", ")})${untilText}`;
    }

    if (byMonthday.length) {
      return interval === 1
        ? `매월 ${byMonthday[0]}일${untilText}`
        : `${interval}개월마다 ${byMonthday[0]}일${untilText}`;
    }

    return `매월 (규칙 미설정)${untilText}`;
  }

  return `${r.freq} 반복`;
}

function generateNextOccurrences({ baseStartISO, recurrence, exceptions, maxCount = 10 }) {
  if (!recurrence?.freq || !baseStartISO) return [];

  const base = new Date(baseStartISO);
  if (Number.isNaN(base.getTime())) return [];

  const until = parseUntilISO(recurrence.untilISO);
  const interval = Math.max(1, Number(recurrence.intervalCount) || 1);
  const exSet = new Set((exceptions || []).filter(Boolean));

  const results = [];
  const pushIfValid = (d) => {
    if (!d) return;
    if (Number.isNaN(d.getTime())) return;
    if (until && d > until) return;
    const key = ymd(d);
    if (exSet.has(key)) return;
    results.push(new Date(d));
  };

  if (recurrence.freq === "DAILY") {
    let cursor = new Date(base);
    cursor.setHours(0, 0, 0, 0);

    while (results.length < maxCount) {
      pushIfValid(cursor);
      cursor = addDays(cursor, interval);
      if (until && cursor > addDays(until, 3650)) break;
    }
    return results;
  }

  if (recurrence.freq === "WEEKLY") {
    const days = Array.isArray(recurrence.byDay) ? recurrence.byDay : [];
    const daySet = new Set(days);
    if (daySet.size === 0) return [];

    const startOfWeekMon = (d) => {
      const x = new Date(d);
      const day = x.getDay();
      const delta = (day === 0 ? 6 : day - 1);
      x.setHours(0, 0, 0, 0);
      x.setDate(x.getDate() - delta);
      return x;
    };

    const baseWeek = startOfWeekMon(base);
    let cursor = new Date(base);
    cursor.setHours(0, 0, 0, 0);

    while (results.length < maxCount) {
      const w0 = startOfWeekMon(cursor);
      const diffWeeks = Math.floor((w0 - baseWeek) / (7 * 24 * 60 * 60 * 1000));
      const okWeek = (diffWeeks % interval) === 0;

      if (okWeek) {
        const curDay = cursor.getDay();
        const curKey =
          curDay === 0 ? "SU" :
          curDay === 1 ? "MO" :
          curDay === 2 ? "TU" :
          curDay === 3 ? "WE" :
          curDay === 4 ? "TH" :
          curDay === 5 ? "FR" : "SA";

        if (daySet.has(curKey)) pushIfValid(cursor);
      }

      cursor = addDays(cursor, 1);
      if (until && cursor > addDays(until, 3650)) break;
    }

    return results;
  }

  if (recurrence.freq === "MONTHLY") {
    const byDay = Array.isArray(recurrence.byDay) ? recurrence.byDay : [];
    const byMonthday = Array.isArray(recurrence.byMonthday) ? recurrence.byMonthday : [];
    const rule = byDay.length ? parseByDayOrdinal(byDay[0]) : null;
    const md = byMonthday.length ? Number(byMonthday[0]) : null;

    const baseYear = base.getFullYear();
    const baseMonth = base.getMonth();

    let k = 0;
    while (results.length < maxCount) {
      const targetMonthDate = new Date(baseYear, baseMonth, 1);
      targetMonthDate.setMonth(targetMonthDate.getMonth() + k * interval);

      const y = targetMonthDate.getFullYear();
      const m = targetMonthDate.getMonth();

      let candidate = null;

      if (rule) {
        candidate = nthWeekdayOfMonth(y, m, WEEKDAY_MAP[rule.day], rule.ord);
      } else if (md != null && Number.isFinite(md)) {
        candidate = new Date(y, m, md);
        if (candidate.getMonth() !== m) candidate = null;
      } else {
        break;
      }

      if (candidate && candidate < base) {
        k += 1;
        continue;
      }

      pushIfValid(candidate);

      k += 1;
      if (until && candidate && candidate > addDays(until, 3650)) break;
    }

    return results;
  }

  return [];
}

export default function RecurrenceViewModal({
  isOpen,
  onClose,
  scheduleStartAtISO,
  recurrence,
  exceptionList,
  onOpenEdit,
}) {
  // ✅ Hook은 항상 동일한 순서로 호출되어야 합니다 (isOpen 체크는 return 직전에)
  const exceptions = useMemo(() => {
    if (!exceptionList) return [];
    if (Array.isArray(exceptionList)) {
      return exceptionList
        .map((x) => (typeof x === "string" ? x : x?.exceptionDate))
        .filter(Boolean);
    }
    return [];
  }, [exceptionList]);

  const summary = useMemo(() => buildSummary(recurrence), [recurrence]);

  const nextDates = useMemo(() => {
    const untilISO = recurrence?.until ? new Date(recurrence.until).toISOString() : null;

    return generateNextOccurrences({
      baseStartISO: scheduleStartAtISO,
      recurrence: {
        freq: recurrence?.freq,
        intervalCount: recurrence?.intervalCount,
        byDay: recurrence?.byDay,
        byMonthday: recurrence?.byMonthday,
        untilISO,
      },
      exceptions,
      maxCount: 10,
    }).map((d) => ymd(d));
  }, [scheduleStartAtISO, recurrence, exceptions]);

  // ✅ 여기에서 조건부 return
  if (!isOpen) return null;

  return (
    <div style={overlayStyle}>
      <div style={modalStyle}>
        <div style={sectionStyle}>
          <div style={labelStyle}>🔍 반복 보기</div>
          <div style={{ fontSize: "0.95rem" }}>{summary}</div>
          <div style={subText}>
            기준 시작일: {scheduleStartAtISO ? String(scheduleStartAtISO) : "알 수 없음"}
          </div>
        </div>

        <div style={sectionStyle}>
          <div style={labelStyle}>다음 10회 미리보기</div>
          {nextDates.length ? (
            <ul style={{ margin: 0, paddingLeft: "1.1rem" }}>
              {nextDates.map((d) => (
                <li key={d} style={{ marginBottom: 4 }}>{d}</li>
              ))}
            </ul>
          ) : (
            <div style={subText}>미리보기 결과가 없습니다. (규칙/요일/날짜 설정을 확인하세요.)</div>
          )}
        </div>

        <div style={sectionStyle}>
          <div style={labelStyle}>예외 날짜</div>
          {exceptions.length ? (
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
              {exceptions.map((d) => (
                <span
                  key={d}
                  style={{
                    fontSize: "0.85rem",
                    background: "#f3f4f6",
                    padding: "4px 8px",
                    borderRadius: 999,
                  }}
                >
                  {d}
                </span>
              ))}
            </div>
          ) : (
            <div style={subText}>예외 날짜 없음</div>
          )}
          <div style={subText}>예외 날짜는 미리보기에서 자동 제외됩니다.</div>
        </div>

        <div style={buttonRow}>
          <button type="button" onClick={onClose} style={btn}>닫기</button>
          {typeof onOpenEdit === "function" ? (
            <button type="button" onClick={onOpenEdit} style={primaryBtn}>반복 수정 열기</button>
          ) : (
            <button type="button" onClick={onClose} style={primaryBtn}>확인</button>
          )}
        </div>
      </div>
    </div>
  );
}
