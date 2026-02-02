// src/pages/CalendarHistoryPage.js
import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiService } from '../../services/apiService';

// ✅ ISO date-time helper (KST 기준으로 문자열 만들기)
const toKstISOString = (date) => {
  // date: JS Date
  // KST(+09:00) 오프셋 붙인 ISO 비슷한 문자열 생성
  const pad = (n) => String(n).padStart(2, '0');

  const yyyy = date.getFullYear();
  const MM = pad(date.getMonth() + 1);
  const dd = pad(date.getDate());
  const hh = pad(date.getHours());
  const mm = pad(date.getMinutes());
  const ss = pad(date.getSeconds());

  return `${yyyy}-${MM}-${dd}T${hh}:${mm}:${ss}+09:00`;
};

const typeLabel = (type) => {
  switch (type) {
    case 'SCHEDULE_CREATED': return '일정 생성';
    case 'SCHEDULE_UPDATED': return '일정 수정';
    case 'SCHEDULE_DELETED': return '일정 삭제';
    case 'CALENDAR_CREATED': return '캘린더 생성';
    case 'CALENDAR_UPDATED': return '캘린더 수정';
    case 'CALENDAR_DELETED': return '캘린더 삭제';
    default: return type;
  }
};

export const CalendarHistoryPage = () => {
  const { calendarId } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [histories, setHistories] = useState([]);
  const [error, setError] = useState(null);

  // ✅ 기본: 최근 7일
  const defaultRange = useMemo(() => {
    const to = new Date();
    const from = new Date();
    from.setDate(to.getDate() - 7);
    return {
      from: toKstISOString(from),
      to: toKstISOString(to),
    };
  }, []);

  const [from, setFrom] = useState(defaultRange.from);
  const [to, setTo] = useState(defaultRange.to);

  const fetchHistories = async () => {
    try {
      setLoading(true);
      setError(null);

      const res = await ApiService.getCalendarHistories(calendarId, { from, to });
      const content = res?.data?.content ?? [];

      setHistories(content);
    } catch (e) {
      console.error(e);
      setError(e?.message || '히스토리 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistories();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [calendarId]);

  return (
    <div style={{ padding: '2rem' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700 }}>🕘 캘린더 기록</h1>
        </div>

        <button
          onClick={() => navigate(-1)}
          style={{
            padding: '0.5rem 1rem',
            backgroundColor: '#fff',
            border: '1px solid #d1d5db',
            borderRadius: '0.5rem',
            cursor: 'pointer',
          }}
        >
          ← 돌아가기
        </button>
      </header>

      {/* ✅ 기간 필터 */}
      <section style={{ marginTop: '1.5rem', display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <div style={{ fontSize: 12, color: '#6b7280' }}>From (date-time)</div>
          <input
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            style={{ padding: '0.5rem', width: 320, border: '1px solid #d1d5db', borderRadius: 8 }}
          />
        </div>

        <div>
          <div style={{ fontSize: 12, color: '#6b7280' }}>To (date-time)</div>
          <input
            value={to}
            onChange={(e) => setTo(e.target.value)}
            style={{ padding: '0.5rem', width: 320, border: '1px solid #d1d5db', borderRadius: 8 }}
          />
        </div>

        <button
          onClick={fetchHistories}
          style={{
            padding: '0.6rem 1rem',
            backgroundColor: '#2563eb',
            color: '#fff',
            border: 'none',
            borderRadius: 10,
            cursor: 'pointer',
            height: 40,
            alignSelf: 'end',
          }}
        >
          조회
        </button>
      </section>

      {/* ✅ 상태 */}
      {loading && <p style={{ marginTop: '1.5rem' }}>불러오는 중...</p>}
      {error && (
        <p style={{ marginTop: '1.5rem', color: 'crimson' }}>
          {error}
        </p>
      )}

      {/* ✅ 리스트 */}
      {!loading && !error && (
        <section style={{ marginTop: '1.5rem' }}>
          {histories.length === 0 ? (
            <p style={{ color: '#6b7280' }}>해당 기간에 기록이 없습니다.</p>
          ) : (
            <div style={{ display: 'grid', gap: 12 }}>
              {histories.map((h) => (
                <div
                  key={h.calendarHistoryId}
                  style={{
                    backgroundColor: '#fff',
                    border: '1px solid #e5e7eb',
                    borderRadius: 12,
                    padding: '1rem',
                    boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                    <div style={{ fontWeight: 700 }}>
                      {typeLabel(h.type)}
                      <span style={{ marginLeft: 10, fontWeight: 400, color: '#6b7280' }}>
                        #{h.calendarHistoryId}
                      </span>
                    </div>
                    <div style={{ color: '#6b7280', fontSize: 13 }}>
                      {new Date(h.createdAt).toLocaleString()}
                    </div>
                  </div>

                  <div style={{ marginTop: 8, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                    <span style={{ fontSize: 13, color: '#6b7280' }}>
                      actorId: <b style={{ color: '#111827' }}>{h.actorId}</b>
                    </span>
                    <span style={{ fontSize: 13, color: '#6b7280' }}>
                      entityId: <b style={{ color: '#111827' }}>{h.entityId}</b>
                    </span>
                    <span style={{ fontSize: 13, color: '#6b7280' }}>
                      calendarId: <b style={{ color: '#111827' }}>{h.calendarId}</b>
                    </span>
                  </div>

                  <div style={{ marginTop: 10 }}>
                    <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 6 }}>
                      changedFields
                    </div>
                    <pre
                      style={{
                        backgroundColor: '#f9fafb',
                        border: '1px solid #e5e7eb',
                        borderRadius: 10,
                        padding: '0.75rem',
                        overflowX: 'auto',
                        fontSize: 12,
                        lineHeight: 1.4,
                      }}
                    >
                      {JSON.stringify(h.changedFields ?? {}, null, 2)}
                    </pre>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
};
