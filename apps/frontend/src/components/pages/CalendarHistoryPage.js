// src/pages/CalendarHistoryPage.js
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ApiService } from '../../services/apiService';

const TYPE = {
  CALENDAR_CREATED: 'CALENDAR_CREATED',
  CALENDAR_UPDATED: 'CALENDAR_UPDATED',
  CALENDAR_DELETED: 'CALENDAR_DELETED',
  CALENDAR_MEMBER_ADDED: 'CALENDAR_MEMBER_ADDED',
  CALENDAR_MEMBER_REMOVED: 'CALENDAR_MEMBER_REMOVED',
  SCHEDULE_CREATED: 'SCHEDULE_CREATED',
  SCHEDULE_UPDATED: 'SCHEDULE_UPDATED',
  SCHEDULE_DELETED: 'SCHEDULE_DELETED',
};

const typeLabel = (type) => {
  switch (type) {
    case TYPE.SCHEDULE_CREATED: return '일정 생성';
    case TYPE.SCHEDULE_UPDATED: return '일정 수정';
    case TYPE.SCHEDULE_DELETED: return '일정 삭제';
    case TYPE.CALENDAR_CREATED: return '캘린더 생성';
    case TYPE.CALENDAR_UPDATED: return '캘린더 수정';
    case TYPE.CALENDAR_DELETED: return '캘린더 삭제';
    case TYPE.CALENDAR_MEMBER_ADDED: return '멤버 추가';
    case TYPE.CALENDAR_MEMBER_REMOVED: return '멤버 삭제';
    default: return type;
  }
};

const formatKst = (iso) => {
  if (!iso) return '';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return String(iso);

  // KST 기준 표기
  return d.toLocaleString('ko-KR', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatScheduleRange = (startAt, endAt) => {
  if (!startAt && !endAt) return '';
  const s = startAt ? formatKst(startAt) : '';
  const e = endAt ? formatKst(endAt) : '';
  if (s && e) return `${s} ~ ${e}`;
  return s || e;
};

// ✅ 명세서 문구 스타일로 메시지 만들기
const buildHistoryMessage = (h) => {
  const actorName = h?.actorName ?? '누군가';
  const targetName = h?.targetName ?? '멤버';
  const scheduleName = h?.scheduleName ?? '일정';
  const range = formatScheduleRange(h?.scheduleStartAt, h?.scheduleEndAt);

  switch (h?.type) {
    case TYPE.CALENDAR_UPDATED:
      return `${actorName}님이 캘린더를 수정했습니다.`;

    case TYPE.CALENDAR_MEMBER_ADDED:
      return `${targetName}님이 캘린더 멤버에 추가되었습니다.`;

    case TYPE.CALENDAR_MEMBER_REMOVED:
      return `${targetName}님이 캘린더 멤버에서 삭제되었습니다.`;

    case TYPE.SCHEDULE_CREATED:
      return `${actorName}님이 일정을 생성했습니다. (${scheduleName}${range ? `, ${range}` : ''})`;

    case TYPE.SCHEDULE_UPDATED:
      return `${actorName}님이 일정을 수정했습니다. (${scheduleName}${range ? `, ${range}` : ''})`;

    case TYPE.SCHEDULE_DELETED:
      return `${actorName}님이 일정을 삭제했습니다. (${scheduleName}${range ? `, ${range}` : ''})`;

    case TYPE.CALENDAR_CREATED:
      return `${actorName}님이 캘린더를 생성했습니다.`;

    case TYPE.CALENDAR_DELETED:
      return `${actorName}님이 캘린더를 삭제했습니다.`;

    default:
      return `${typeLabel(h?.type)} 기록이 발생했습니다.`;
  }
};

export const CalendarHistoryPage = () => {
  const { calendarId } = useParams();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [histories, setHistories] = useState([]);
  const [error, setError] = useState(null);

  // ✅ 페이지네이션 상태
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const pageMetaDefault = useMemo(() => ({
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    hasNext: false,
    hasPrevious: false,
  }), []);

  const [pageMeta, setPageMeta] = useState(pageMetaDefault);

  const fetchHistories = useCallback(async () => {
    if (!calendarId) return;

    try {
      setLoading(true);
      setError(null);

      // ✅ 명세서: PathVariable(calendarId) + (서버가 지원하면) page/size 쿼리
      const res = await ApiService.getCalendarHistories(calendarId, { page, size });

      // axios 기준: res.data 가 최상위( code/status/message/data )
      const body = res?.data ?? {};
      const data = body?.data ?? body; // 혹시 백엔드/프록시에서 한 겹 덜 감쌀 때 대비

      const content = data?.content ?? [];
      setHistories(content);

      setPageMeta({
        page: data?.page ?? page,
        size: data?.size ?? size,
        totalElements: data?.totalElements ?? 0,
        totalPages: data?.totalPages ?? 0,
        first: data?.first ?? (data?.page === 0),
        last: data?.last ?? false,
        hasNext: data?.hasNext ?? false,
        hasPrevious: data?.hasPrevious ?? false,
      });
    } catch (e) {
      console.error(e);
      setError(e?.message || '히스토리 조회 실패');
    } finally {
      setLoading(false);
    }
  }, [calendarId, page, size]);

  useEffect(() => {
    fetchHistories();
  }, [fetchHistories]);

  const onPrev = () => {
    if (pageMeta.hasPrevious && page > 0) setPage((p) => p - 1);
  };

  const onNext = () => {
    if (pageMeta.hasNext) setPage((p) => p + 1);
  };

  const onChangeSize = (e) => {
    const nextSize = Number(e.target.value);
    setSize(nextSize);
    setPage(0); // ✅ size 바꾸면 첫 페이지로
  };

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

      {/* ✅ 상단 컨트롤: 페이지네이션 */}
      <section
        style={{
          marginTop: '1.5rem',
          display: 'flex',
          gap: 12,
          alignItems: 'center',
          flexWrap: 'wrap',
        }}
      >
        <button
          onClick={onPrev}
          disabled={!pageMeta.hasPrevious || page === 0}
          style={{
            padding: '0.55rem 0.9rem',
            backgroundColor: (!pageMeta.hasPrevious || page === 0) ? '#f3f4f6' : '#fff',
            border: '1px solid #d1d5db',
            borderRadius: 10,
            cursor: (!pageMeta.hasPrevious || page === 0) ? 'not-allowed' : 'pointer',
          }}
        >
          ← 이전
        </button>

        <button
          onClick={onNext}
          disabled={!pageMeta.hasNext}
          style={{
            padding: '0.55rem 0.9rem',
            backgroundColor: !pageMeta.hasNext ? '#f3f4f6' : '#fff',
            border: '1px solid #d1d5db',
            borderRadius: 10,
            cursor: !pageMeta.hasNext ? 'not-allowed' : 'pointer',
          }}
        >
          다음 →
        </button>

        <div style={{ marginLeft: 6, color: '#6b7280', fontSize: 13 }}>
          page <b style={{ color: '#111827' }}>{pageMeta.page}</b> /{' '}
          <b style={{ color: '#111827' }}>{Math.max(pageMeta.totalPages - 1, 0)}</b>
          <span style={{ marginLeft: 10 }}>
            total <b style={{ color: '#111827' }}>{pageMeta.totalElements}</b>
          </span>
        </div>

        <div style={{ marginLeft: 'auto', display: 'flex', gap: 10, alignItems: 'center' }}>
          <div style={{ fontSize: 12, color: '#6b7280' }}>개수</div>
          <select
            value={size}
            onChange={onChangeSize}
            style={{
              padding: '0.5rem',
              border: '1px solid #d1d5db',
              borderRadius: 10,
              backgroundColor: '#fff',
              cursor: 'pointer',
            }}
          >
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={50}>50</option>
          </select>

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
            }}
          >
            새로고침
          </button>
        </div>
      </section>

      {/* ✅ 상태 */}
      {loading && <p style={{ marginTop: '1.5rem' }}>불러오는 중...</p>}
      {error && <p style={{ marginTop: '1.5rem', color: 'crimson' }}>{error}</p>}

      {/* ✅ 리스트 */}
      {!loading && !error && (
        <section style={{ marginTop: '1.5rem' }}>
          {histories.length === 0 ? (
            <p style={{ color: '#6b7280' }}>기록이 없습니다.</p>
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
                      {formatKst(h.createdAt)}
                    </div>
                  </div>

                  {/* ✅ 명세서 문장형 */}
                  <div style={{ marginTop: 10, fontSize: 14, color: '#111827' }}>
                    {buildHistoryMessage(h)}
                  </div>

                  {/* ✅ (옵션) 디버그/상세 필드 표시 - 명세서 항목만 */}
                  <div style={{ marginTop: 10, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
                    {h.actorName && (
                      <span style={{ fontSize: 12, color: '#6b7280' }}>
                        actorName: <b style={{ color: '#111827' }}>{h.actorName}</b>
                      </span>
                    )}
                    {h.targetName && (
                      <span style={{ fontSize: 12, color: '#6b7280' }}>
                        targetName: <b style={{ color: '#111827' }}>{h.targetName}</b>
                      </span>
                    )}
                    {h.scheduleName && (
                      <span style={{ fontSize: 12, color: '#6b7280' }}>
                        scheduleName: <b style={{ color: '#111827' }}>{h.scheduleName}</b>
                      </span>
                    )}
                    {(h.scheduleStartAt || h.scheduleEndAt) && (
                      <span style={{ fontSize: 12, color: '#6b7280' }}>
                        scheduleAt:{' '}
                        <b style={{ color: '#111827' }}>
                          {formatScheduleRange(h.scheduleStartAt, h.scheduleEndAt)}
                        </b>
                      </span>
                    )}
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
