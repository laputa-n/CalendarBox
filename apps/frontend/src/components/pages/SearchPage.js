import React, { useState, useEffect } from 'react';
import { Check , Calendar , Loader2 , Plus , Edit , Trash2 , Search, Clock, MapPin, Calendar as CalendarIcon, Filter, X } from 'lucide-react';
import { useSchedules } from '../../contexts/ScheduleContext';
import { formatDate, formatTime , formatDateTime } from '../../utils/dateUtils';
import { LoadingSpinner } from '../common/LoadingSpinner';
import { ApiService } from '../../services/apiService';
import { useCalendars } from '../../contexts/CalendarContext';

export const SearchPage = () => {
  const { searchSchedules, loading } = useSchedules();
  const { currentCalendar } = useCalendars();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [filters, setFilters] = useState({
    dateFrom: '',
    dateTo: '',
    location: '',
    category: ''
  });
  const [showFilters, setShowFilters] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [invited, setInvited] = useState([]);
  const [invitedLoading, setInvitedLoading] = useState(false);
  const [invitedPage, setInvitedPage] = useState({ page: 0, size: 20, totalPages: 0, totalElements: 0 });
  const [respondingId, setRespondingId] = useState(null);
  const [selectedSchedule, setSelectedSchedule] = useState(null);

  useEffect(() => {
    if (searchQuery.trim()) {
      const debounceTimer = setTimeout(() => {
        handleSearch();
      }, 500);
      return () => clearTimeout(debounceTimer);
    } else {
      setSearchResults([]);
      setHasSearched(false);
    }
  }, [searchQuery, filters]);

  const handleSearch = async () => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      setHasSearched(false);
      return;
    }

    try {
      setHasSearched(true);
      const results = await searchSchedules(searchQuery, filters);
      setSearchResults(results);
    } catch (error) {
      console.error('Search failed:', error);
      setSearchResults([]);
    }
  };

  const fetchInvited = async (page = 0) => {
  try {
    setInvitedLoading(true);

    const res = await ApiService.getInvitedSchedules(page, invitedPage.size);

    const payload = res?.data ?? res;
    const data = payload?.data ?? payload;
    const content = data?.content ?? [];
    setInvited(Array.isArray(content) ? content : []);

    setInvitedPage({
      page: data?.page ?? page,
      size: data?.size ?? invitedPage.size,
      totalPages: data?.totalPages ?? 0,
      totalElements: data?.totalElements ?? 0,
    });
  } catch (e) {
    console.error('[getInvitedSchedules] failed', e);
    setInvited([]);
  } finally {
    setInvitedLoading(false);
  }
};

// ✅ 수락/거절
const handleRespondInvite = async (inv, action) => {
  try {
    setRespondingId(inv.scheduleParticipantId);
    await ApiService.respondScheduleInvite(inv.scheduleId, inv.scheduleParticipantId, action);

    // 화면에서 제거
    setInvited((prev) => prev.filter((x) => x.scheduleParticipantId !== inv.scheduleParticipantId));

  } catch (e) {
    console.error('[respondScheduleInvite] failed', e);
    alert('초대 응답 실패');
  } finally {
    setRespondingId(null);
  }
};

  const clearFilters = () => {
    setFilters({
      dateFrom: '',
      dateTo: '',
      location: '',
      category: ''
    });
  };

  const hasActiveFilters = Object.values(filters).some(value => value !== '');

  const cardStyle = {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb'
  };

  useEffect(() => {
  if (!currentCalendar?.id) {
    setInvited([]);
    return;
  }
  fetchInvited(0);
}, [currentCalendar?.id]);

  return (
    <div>
      <div style={{ marginBottom: '1.5rem' }}>
        <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>
          검색
        </h1>
        <p style={{ color: '#6b7280' }}>
          일정을 빠르게 찾아보세요
        </p>
      </div>
      {/* ✅ 받은 초대 목록 (캘린더 선택 시에만 표시) */}
{currentCalendar && (
  <div style={{ ...cardStyle, marginBottom: '1rem' }}>
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
      <h3 style={{ margin: 0, fontSize: '1rem', fontWeight: 700 }}>받은 일정 초대</h3>
      <button
        type="button"
        onClick={() => fetchInvited(0)}
        style={{ padding: '6px 10px', borderRadius: 8, border: '1px solid #e5e7eb', background: '#fff', cursor: 'pointer' }}
        disabled={invitedLoading}
      >
        새로고침
      </button>
    </div>

    {invitedLoading ? (
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', color: '#6b7280' }}>
        <Loader2 style={{ width: 16, height: 16, animation: 'spin 1s linear infinite' }} />
        불러오는 중...
      </div>
    ) : invited.length === 0 ? (
      <div style={{ color: '#6b7280', fontSize: 14 }}>받은 초대가 없습니다.</div>
    ) : (
      <>
        {invited.map((inv) => (
          <div
            key={inv.scheduleParticipantId}
            style={{
              border: '1px solid #e5e7eb',
              borderRadius: 10,
              padding: 12,
              marginBottom: 8,
              display: 'flex',
              justifyContent: 'space-between',
              gap: 12,
              background: '#fafafa',
            }}
          >
            <div style={{ minWidth: 0 }}>
              <div style={{ fontWeight: 700, marginBottom: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {inv.scheduleTitle}
              </div>
              <div style={{ fontSize: 13, color: '#6b7280' }}>
                초대자: {inv.inviterName} (id: {inv.inviterId})
              </div>
              <div style={{ fontSize: 13, color: '#6b7280', marginTop: 4 }}>
                {formatDateTime(inv.startAt)} - {formatDateTime(inv.endAt)}
              </div>

              <button
                type="button"
                onClick={() => setSelectedSchedule(inv.scheduleId)} // ✅ 상세 보기 열기(이미 있는 모달 재사용)
                style={{
                  marginTop: 8,
                  padding: '6px 10px',
                  borderRadius: 8,
                  border: '1px solid #e5e7eb',
                  background: '#fff',
                  cursor: 'pointer',
                  fontSize: 12,
                }}
              >
                상세 보기
              </button>
            </div>

            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <button
                type="button"
                onClick={() => handleRespondInvite(inv, 'ACCEPT')}
                disabled={respondingId === inv.scheduleParticipantId}
                style={{
                  padding: '8px 10px',
                  borderRadius: 8,
                  border: 'none',
                  cursor: 'pointer',
                  background: '#2563eb',
                  color: '#fff',
                  opacity: respondingId === inv.scheduleParticipantId ? 0.6 : 1,
                }}
                title="수락"
              >
                <Check size={16} />
              </button>

              <button
                type="button"
                onClick={() => handleRespondInvite(inv, 'REJECT')}
                disabled={respondingId === inv.scheduleParticipantId}
                style={{
                  padding: '8px 10px',
                  borderRadius: 8,
                  border: 'none',
                  cursor: 'pointer',
                  background: '#ef4444',
                  color: '#fff',
                  opacity: respondingId === inv.scheduleParticipantId ? 0.6 : 1,
                }}
                title="거절"
              >
                <X size={16} />
              </button>
            </div>
          </div>
        ))}

        {/* ✅ 간단 페이징 */}
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
          <button
            type="button"
            onClick={() => fetchInvited(Math.max(0, invitedPage.page - 1))}
            disabled={invitedPage.page <= 0 || invitedLoading}
            style={{ padding: '6px 10px', borderRadius: 8, border: '1px solid #e5e7eb', background: '#fff' }}
          >
            이전
          </button>

          <div style={{ fontSize: 12, color: '#6b7280' }}>
            {invitedPage.page + 1} / {Math.max(1, invitedPage.totalPages)}
          </div>

          <button
            type="button"
            onClick={() => fetchInvited(invitedPage.page + 1)}
            disabled={invitedPage.page + 1 >= invitedPage.totalPages || invitedLoading}
            style={{ padding: '6px 10px', borderRadius: 8, border: '1px solid #e5e7eb', background: '#fff' }}
          >
            다음
          </button>
        </div>
      </>
    )}
  </div>
)}

      <div style={cardStyle}>
        {/* 검색 입력 */}
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
          <div style={{ flex: 1, position: 'relative' }}>
            <Search style={{ 
              position: 'absolute', 
              left: '0.75rem', 
              top: '50%', 
              transform: 'translateY(-50%)', 
              width: '1.25rem', 
              height: '1.25rem', 
              color: '#6b7280' 
            }} />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="일정 제목, 설명, 장소로 검색하세요..."
              style={{
                width: '100%',
                padding: '0.75rem 0.75rem 0.75rem 2.5rem',
                border: '1px solid #d1d5db',
                borderRadius: '0.5rem',
                fontSize: '0.875rem',
                outline: 'none'
              }}
              onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            />
          </div>
          <button 
            onClick={() => setShowFilters(!showFilters)}
            style={{
              padding: '0.75rem',
              border: `1px solid ${hasActiveFilters ? '#3b82f6' : '#d1d5db'}`,
              borderRadius: '0.5rem',
              backgroundColor: hasActiveFilters ? '#3b82f6' : 'white',
              color: hasActiveFilters ? 'white' : '#374151',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem'
            }}
          >
            <Filter style={{ width: '1.25rem', height: '1.25rem' }} />
            필터
          </button>
        </div>

        {/* 필터 패널 */}
        {showFilters && (
          <div style={{
            padding: '1rem',
            backgroundColor: '#f9fafb',
            borderRadius: '0.5rem',
            marginBottom: '1rem'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1rem', fontWeight: '600', margin: 0 }}>검색 필터</h3>
              {hasActiveFilters && (
                <button
                  onClick={clearFilters}
                  style={{
                    padding: '0.25rem 0.5rem',
                    fontSize: '0.75rem',
                    color: '#6b7280',
                    backgroundColor: 'transparent',
                    border: 'none',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.25rem'
                  }}
                >
                  <X style={{ width: '0.875rem', height: '0.875rem' }} />
                  필터 초기화
                </button>
              )}
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  시작 날짜
                </label>
                <input
                  type="date"
                  value={filters.dateFrom}
                  onChange={(e) => setFilters({...filters, dateFrom: e.target.value})}
                  style={{
                    width: '100%',
                    padding: '0.5rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
              
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  종료 날짜
                </label>
                <input
                  type="date"
                  value={filters.dateTo}
                  onChange={(e) => setFilters({...filters, dateTo: e.target.value})}
                  style={{
                    width: '100%',
                    padding: '0.5rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
              
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  장소
                </label>
                <input
                  type="text"
                  value={filters.location}
                  onChange={(e) => setFilters({...filters, location: e.target.value})}
                  placeholder="장소로 필터링"
                  style={{
                    width: '100%',
                    padding: '0.5rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                    fontSize: '0.875rem'
                  }}
                />
              </div>
              
              <div>
                <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: '500', marginBottom: '0.5rem' }}>
                  카테고리
                </label>
                <select
                  value={filters.category}
                  onChange={(e) => setFilters({...filters, category: e.target.value})}
                  style={{
                    width: '100%',
                    padding: '0.5rem',
                    border: '1px solid #d1d5db',
                    borderRadius: '0.5rem',
                    fontSize: '0.875rem'
                  }}
                >
                  <option value="">전체 카테고리</option>
                  <option value="work">업무</option>
                  <option value="personal">개인</option>
                  <option value="meeting">미팅</option>
                  <option value="other">기타</option>
                </select>
              </div>
            </div>
          </div>
        )}
        
        {/* 검색 결과 */}
        {loading ? (
          <LoadingSpinner text="검색 중..." />
        ) : hasSearched && (
          <div>
            <h4 style={{ 
              fontSize: '1rem', 
              fontWeight: '600', 
              marginBottom: '1rem',
              color: '#1f2937'
            }}>
              검색 결과 ({searchResults.length}개)
            </h4>
            
            {searchResults.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {searchResults.map(schedule => (
                  <div key={schedule.id} style={{
                    padding: '1rem',
                    borderRadius: '0.5rem',
                    backgroundColor: '#f8fafc',
                    border: `1px solid #e2e8f0`,
                    cursor: 'pointer',
                    transition: 'all 0.2s'
                  }}
                  onMouseOver={(e) => {
                    e.currentTarget.style.backgroundColor = '#f1f5f9';
                    e.currentTarget.style.borderColor = '#cbd5e1';
                  }}
                  onMouseOut={(e) => {
                    e.currentTarget.style.backgroundColor = '#f8fafc';
                    e.currentTarget.style.borderColor = '#e2e8f0';
                  }}>
                    <div style={{ display: 'flex', alignItems: 'start', gap: '0.75rem' }}>
                      <div style={{
                        width: '1rem',
                        height: '1rem',
                        backgroundColor: schedule.color || '#3b82f6',
                        borderRadius: '50%',
                        marginTop: '0.125rem',
                        flexShrink: 0
                      }} />
                      
                      <div style={{ flex: 1 }}>
                        <h5 style={{ 
                          fontWeight: '600', 
                          color: '#1f2937', 
                          fontSize: '0.875rem', 
                          margin: '0 0 0.5rem 0' 
                        }}>
                          {schedule.title}
                        </h5>
                        
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                          <div style={{ display: 'flex', alignItems: 'center', fontSize: '0.75rem', color: '#6b7280' }}>
                            <Clock style={{ width: '0.875rem', height: '0.875rem', marginRight: '0.5rem' }} />
                            <span>
                              {formatDate(schedule.startDateTime)} {formatTime(schedule.startDateTime)}
                            </span>
                          </div>
                          
                          {schedule.location && (
                            <div style={{ display: 'flex', alignItems: 'center', fontSize: '0.75rem', color: '#6b7280' }}>
                              <MapPin style={{ width: '0.875rem', height: '0.875rem', marginRight: '0.5rem' }} />
                              <span>{schedule.location}</span>
                            </div>
                          )}
                          
                          {schedule.description && (
                            <p style={{ 
                              fontSize: '0.75rem', 
                              color: '#6b7280', 
                              margin: '0.25rem 0 0 0',
                              lineHeight: '1.4'
                            }}>
                              {schedule.description.length > 100 
                                ? `${schedule.description.substring(0, 100)}...` 
                                : schedule.description
                              }
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ 
                padding: '2rem', 
                textAlign: 'center', 
                color: '#6b7280',
                backgroundColor: '#f9fafb',
                borderRadius: '0.5rem'
              }}>
                <Search style={{ width: '2rem', height: '2rem', color: '#d1d5db', margin: '0 auto 0.5rem auto' }} />
                <p style={{ fontSize: '0.875rem', margin: 0 }}>
                  "{searchQuery}"에 대한 검색 결과가 없습니다.
                </p>
                <p style={{ fontSize: '0.75rem', color: '#9ca3af', margin: '0.25rem 0 0 0' }}>
                  다른 키워드로 검색해보거나 필터를 조정해보세요.
                </p>
              </div>
            )}
          </div>
        )}
        
        {!hasSearched && !searchQuery && (
          <div style={{ 
            padding: '3rem', 
            textAlign: 'center', 
            color: '#6b7280'
          }}>
            <Search style={{ width: '3rem', height: '3rem', color: '#d1d5db', margin: '0 auto 1rem auto' }} />
            <p style={{ fontSize: '1rem', marginBottom: '0.5rem' }}>일정을 검색해보세요</p>
            <p style={{ fontSize: '0.875rem', color: '#9ca3af' }}>
              제목, 설명, 장소 등으로 원하는 일정을 찾을 수 있습니다.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};