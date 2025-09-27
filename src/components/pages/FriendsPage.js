import React, { useState, useEffect } from 'react';
import { 
  Users, 
  UserPlus, 
  User, 
  CheckCircle, 
  XCircle, 
  Trash2, 
  Search,
  Send,
  Clock,
  Mail
} from 'lucide-react';
import { useFriends } from '../../contexts/FriendContext';
import { validateEmail } from '../../utils/validationUtils';
import { LoadingSpinner } from '../common/LoadingSpinner';

export const FriendsPage = () => {
  // 기존 데이터
  const { 
    pendingFriendships, 
    acceptedFriendships, 
    sendFriendRequest, 
    acceptFriendship, 
    rejectFriendship,
    removeFriend,
    loading,
    
    // 새로운 데이터와 메서드들
    receivedRequests,
    sentRequests,
    searchResults,
    searchLoading,
    sendFriendRequestById,
    acceptFriendRequest,
    rejectFriendRequest,
    fetchReceivedRequests,
    fetchSentRequests,
    searchUsers
  } = useFriends();
  
  // 상태 관리
  const [activeTab, setActiveTab] = useState('friends'); // friends, search, received, sent
  const [showAddForm, setShowAddForm] = useState(false);
  const [friendEmail, setFriendEmail] = useState('');
  const [emailError, setEmailError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // 탭 변경시 데이터 로드
  useEffect(() => {
    if (activeTab === 'received') {
      fetchReceivedRequests();
    } else if (activeTab === 'sent') {
      fetchSentRequests();
    }
  }, [activeTab]);

  // 검색 처리
  const handleSearch = (query) => {
    setSearchQuery(query);
    if (query.trim()) {
      searchUsers(query);
    }
  };

  // 기존 이메일 기반 친구 추가
  const handleAddFriend = async (e) => {
    e.preventDefault();
    
    if (!validateEmail(friendEmail)) {
      setEmailError('올바른 이메일 주소를 입력해주세요.');
      return;
    }
    
    setEmailError('');
    
    try {
      await sendFriendRequest(friendEmail);
      setFriendEmail('');
      setShowAddForm(false);
    } catch (error) {
      console.error('Failed to add friend:', error);
    }
  };

  // ID 기반 친구 요청 보내기
  const handleSendRequest = async (userId) => {
    try {
      await sendFriendRequestById(userId);
      // 검색 결과 새로고침하거나 성공 메시지 표시
    } catch (error) {
      console.error('Failed to send friend request:', error);
    }
  };

  const cardStyle = {
    backgroundColor: 'white',
    padding: '1.5rem',
    borderRadius: '0.75rem',
    boxShadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
    border: '1px solid #e5e7eb'
  };

  const buttonStyle = (bgColor = '#2563eb', textColor = 'white') => ({
    backgroundColor: bgColor,
    color: textColor,
    padding: '0.75rem 1rem',
    borderRadius: '0.5rem',
    border: 'none',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    fontSize: '0.875rem',
    fontWeight: '500'
  });

  const tabStyle = (isActive) => ({
    padding: '0.75rem 1rem',
    borderRadius: '0.5rem',
    border: 'none',
    cursor: 'pointer',
    fontSize: '0.875rem',
    fontWeight: '500',
    backgroundColor: isActive ? '#2563eb' : '#f3f4f6',
    color: isActive ? 'white' : '#374151'
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', color: '#1f2937', marginBottom: '0.5rem' }}>
            친구 관리
          </h1>
          <p style={{ color: '#6b7280' }}>
            친구들과 일정을 공유하고 함께 계획하세요
          </p>
        </div>
        <button 
          onClick={() => setShowAddForm(true)}
          style={buttonStyle()}
        >
          <UserPlus style={{ width: '1.25rem', height: '1.25rem' }} />
          친구 추가
        </button>
      </div>

      {/* 탭 네비게이션 */}
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
        <button 
          onClick={() => setActiveTab('friends')} 
          style={tabStyle(activeTab === 'friends')}
        >
          <Users style={{ width: '1rem', height: '1rem' }} />
          친구 목록
        </button>
        <button 
          onClick={() => setActiveTab('search')} 
          style={tabStyle(activeTab === 'search')}
        >
          <Search style={{ width: '1rem', height: '1rem' }} />
          친구 찾기
        </button>
        <button 
          onClick={() => setActiveTab('received')} 
          style={tabStyle(activeTab === 'received')}
        >
          <Mail style={{ width: '1rem', height: '1rem' }} />
          받은 요청 ({receivedRequests.content?.filter(r => r.status === 'PENDING').length || 0})
        </button>
        <button 
          onClick={() => setActiveTab('sent')} 
          style={tabStyle(activeTab === 'sent')}
        >
          <Send style={{ width: '1rem', height: '1rem' }} />
          보낸 요청 ({sentRequests.content?.length || 0})
        </button>
      </div>

      {/* 친구 추가 폼 (이메일 기반) */}
      {showAddForm && (
        <div style={{ ...cardStyle, marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            이메일로 친구 추가
          </h3>
          <form onSubmit={handleAddFriend}>
            <div>
              <input
                type="email"
                value={friendEmail}
                onChange={(e) => {
                  setFriendEmail(e.target.value);
                  setEmailError('');
                }}
                placeholder="친구의 이메일을 입력하세요"
                required
                style={{
                  width: '100%',
                  padding: '0.75rem',
                  border: `1px solid ${emailError ? '#dc2626' : '#d1d5db'}`,
                  borderRadius: '0.5rem',
                  fontSize: '0.875rem',
                  marginBottom: emailError ? '0.25rem' : '1rem'
                }}
              />
              {emailError && (
                <p style={{ color: '#dc2626', fontSize: '0.75rem', margin: '0 0 1rem 0' }}>
                  {emailError}
                </p>
              )}
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button type="submit" style={buttonStyle()} disabled={loading}>
                  {loading ? '추가 중...' : '친구 요청 보내기'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowAddForm(false);
                    setFriendEmail('');
                    setEmailError('');
                  }}
                  style={buttonStyle('#6b7280', 'white')}
                >
                  취소
                </button>
              </div>
            </div>
          </form>
        </div>
      )}

      {/* 탭 콘텐츠 */}
      {activeTab === 'friends' && (
        <>
          {/* 기존 친구 요청 (PENDING) */}
          {pendingFriendships.length > 0 && (
            <div style={{ ...cardStyle, marginBottom: '1.5rem' }}>
              <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
                친구 요청 ({pendingFriendships.length}개)
              </h3>
              {pendingFriendships.map((friendship, index) => (
                <div key={friendship.id} style={{
                  padding: '1rem',
                  borderBottom: index < pendingFriendships.length - 1 ? '1px solid #e5e7eb' : 'none',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <div style={{
                      width: '3rem',
                      height: '3rem',
                      backgroundColor: '#fef3c7',
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      <User style={{ width: '1.5rem', height: '1.5rem', color: '#d97706' }} />
                    </div>
                    <div>
                      <h4 style={{ fontSize: '1rem', fontWeight: '600', color: '#1f2937', margin: 0 }}>
                        {friendship.requesterUser?.name || '알 수 없음'}
                      </h4>
                      <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: '0.25rem 0' }}>
                        {friendship.requesterUser?.email || '이메일 없음'}
                      </p>
                    </div>
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button
                      onClick={() => acceptFriendship(friendship.id)}
                      style={buttonStyle('#10b981')}
                      disabled={loading}
                    >
                      <CheckCircle style={{ width: '1rem', height: '1rem' }} />
                      승인
                    </button>
                    <button
                      onClick={() => rejectFriendship(friendship.id)}
                      style={buttonStyle('#dc2626')}
                      disabled={loading}
                    >
                      <XCircle style={{ width: '1rem', height: '1rem' }} />
                      거절
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* 친구 목록 */}
          <div style={cardStyle}>
            <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
              친구 목록 ({acceptedFriendships.length}명)
            </h3>
            {loading ? (
              <LoadingSpinner text="친구 목록을 불러오는 중..." />
            ) : acceptedFriendships.length > 0 ? (
              acceptedFriendships.map((friendship, index) => (
                <div key={friendship.id} style={{
                  padding: '1.5rem',
                  borderBottom: index < acceptedFriendships.length - 1 ? '1px solid #e5e7eb' : 'none',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <div style={{
                      width: '3rem',
                      height: '3rem',
                      backgroundColor: '#dcfce7',
                      borderRadius: '50%',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      <User style={{ width: '1.5rem', height: '1.5rem', color: '#16a34a' }} />
                    </div>
                    <div>
                      <h4 style={{ fontSize: '1.125rem', fontWeight: '600', color: '#1f2937', margin: 0 }}>
                        {friendship.requesterUser?.name || '알 수 없음'}
                      </h4>
                      <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: '0.25rem 0' }}>
                        {friendship.requesterUser?.email || '이메일 없음'}
                      </p>
                      <p style={{ fontSize: '0.75rem', color: '#9ca3af', margin: 0 }}>
                        친구가 된 날: {new Date(friendship.createdAt).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => {
                      if (window.confirm('정말로 친구를 삭제하시겠습니까?')) {
                        removeFriend(friendship.id);
                      }
                    }}
                    style={{
                      padding: '0.5rem',
                      color: '#dc2626',
                      backgroundColor: 'transparent',
                      border: 'none',
                      borderRadius: '0.5rem',
                      cursor: 'pointer'
                    }}
                    disabled={loading}
                    title="친구 삭제"
                  >
                    <Trash2 style={{ width: '1rem', height: '1rem' }} />
                  </button>
                </div>
              ))
            ) : (
              <div style={{ padding: '3rem', textAlign: 'center', color: '#6b7280' }}>
                <Users style={{ width: '4rem', height: '4rem', color: '#d1d5db', margin: '0 auto 1rem auto' }} />
                <p style={{ fontSize: '1.125rem', marginBottom: '0.5rem' }}>아직 친구가 없습니다.</p>
                <p style={{ color: '#9ca3af' }}>친구를 추가하여 일정을 공유해보세요!</p>
              </div>
            )}
          </div>
        </>
      )}

      {/* 친구 찾기 탭 */}
      {activeTab === 'search' && (
        <div style={cardStyle}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            친구 찾기
          </h3>
          <div style={{ marginBottom: '1rem' }}>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => handleSearch(e.target.value)}
              placeholder="이름 또는 이메일로 검색..."
              style={{
                width: '100%',
                padding: '0.75rem',
                border: '1px solid #d1d5db',
                borderRadius: '0.5rem',
                fontSize: '0.875rem'
              }}
            />
          </div>
          
          {searchLoading ? (
            <LoadingSpinner text="사용자를 검색하는 중..." />
          ) : searchResults.length > 0 ? (
            searchResults.map((user) => (
              <div key={user.id} style={{
                padding: '1rem',
                borderBottom: '1px solid #e5e7eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <div style={{
                    width: '3rem',
                    height: '3rem',
                    backgroundColor: '#e0e7ff',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}>
                    <User style={{ width: '1.5rem', height: '1.5rem', color: '#6366f1' }} />
                  </div>
                  <div>
                    <h4 style={{ fontSize: '1rem', fontWeight: '600', color: '#1f2937', margin: 0 }}>
                      {user.name}
                    </h4>
                    <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: '0.25rem 0' }}>
                      {user.email}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => handleSendRequest(user.id)}
                  style={buttonStyle()}
                  disabled={loading}
                >
                  <UserPlus style={{ width: '1rem', height: '1rem' }} />
                  친구 요청
                </button>
              </div>
            ))
          ) : searchQuery ? (
            <div style={{ padding: '2rem', textAlign: 'center', color: '#6b7280' }}>
              <p>검색 결과가 없습니다.</p>
            </div>
          ) : (
            <div style={{ padding: '2rem', textAlign: 'center', color: '#6b7280' }}>
              <Search style={{ width: '3rem', height: '3rem', color: '#d1d5db', margin: '0 auto 1rem auto' }} />
              <p>친구를 검색해보세요!</p>
            </div>
          )}
        </div>
      )}

      {/* 받은 요청 탭 */}
      {activeTab === 'received' && (
        <div style={cardStyle}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            받은 친구 요청
          </h3>
          {receivedRequests.loading ? (
            <LoadingSpinner text="받은 요청을 불러오는 중..." />
          ) : receivedRequests.content?.length > 0 ? (
            receivedRequests.content.map((request) => (
              <div key={request.friendshipId} style={{
                padding: '1rem',
                borderBottom: '1px solid #e5e7eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <div style={{
                    width: '3rem',
                    height: '3rem',
                    backgroundColor: '#fef3c7',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}>
                    <User style={{ width: '1.5rem', height: '1.5rem', color: '#d97706' }} />
                  </div>
                  <div>
                    <h4 style={{ fontSize: '1rem', fontWeight: '600', color: '#1f2937', margin: 0 }}>
                      사용자 ID: {request.requesterId}
                    </h4>
                    <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: '0.25rem 0' }}>
                      {new Date(request.createdAt).toLocaleDateString('ko-KR')}
                    </p>
                    <p style={{ fontSize: '0.75rem', color: '#9ca3af', margin: 0 }}>
                      상태: {request.status}
                    </p>
                  </div>
                </div>
                {request.status === 'PENDING' && (
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button
                      onClick={() => acceptFriendRequest(request.friendshipId)}
                      style={buttonStyle('#10b981')}
                      disabled={loading}
                    >
                      <CheckCircle style={{ width: '1rem', height: '1rem' }} />
                      수락
                    </button>
                    <button
                      onClick={() => rejectFriendRequest(request.friendshipId)}
                      style={buttonStyle('#dc2626')}
                      disabled={loading}
                    >
                      <XCircle style={{ width: '1rem', height: '1rem' }} />
                      거절
                    </button>
                  </div>
                )}
              </div>
            ))
          ) : (
            <div style={{ padding: '3rem', textAlign: 'center', color: '#6b7280' }}>
              <Mail style={{ width: '4rem', height: '4rem', color: '#d1d5db', margin: '0 auto 1rem auto' }} />
              <p>받은 친구 요청이 없습니다.</p>
            </div>
          )}
        </div>
      )}

      {/* 보낸 요청 탭 */}
      {activeTab === 'sent' && (
        <div style={cardStyle}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            보낸 친구 요청
          </h3>
          {sentRequests.loading ? (
            <LoadingSpinner text="보낸 요청을 불러오는 중..." />
          ) : sentRequests.content?.length > 0 ? (
            sentRequests.content.map((request) => (
              <div key={request.friendshipId} style={{
                padding: '1rem',
                borderBottom: '1px solid #e5e7eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <div style={{
                    width: '3rem',
                    height: '3rem',
                    backgroundColor: '#e0e7ff',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}>
                    <User style={{ width: '1.5rem', height: '1.5rem', color: '#6366f1' }} />
                  </div>
                  <div>
                    <h4 style={{ fontSize: '1rem', fontWeight: '600', color: '#1f2937', margin: 0 }}>
                      사용자 ID: {request.addresseeId}
                    </h4>
                    <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: '0.25rem 0' }}>
                      {new Date(request.createdAt).toLocaleDateString('ko-KR')}
                    </p>
                    <p style={{ fontSize: '0.75rem', color: '#9ca3af', margin: 0 }}>
                      상태: {request.status}
                    </p>
                  </div>
                </div>
                <div style={{
                  padding: '0.5rem 1rem',
                  borderRadius: '1rem',
                  fontSize: '0.75rem',
                  fontWeight: '500',
                  backgroundColor: request.status === 'accept' ? '#dcfce7' : '#fef3c7',
                  color: request.status === 'accept' ? '#16a34a' : '#d97706'
                }}>
                  {request.status === 'accept' ? '수락됨' : '대기중'}
                </div>
              </div>
            ))
          ) : (
            <div style={{ padding: '3rem', textAlign: 'center', color: '#6b7280' }}>
              <Send style={{ width: '4rem', height: '4rem', color: '#d1d5db', margin: '0 auto 1rem auto' }} />
              <p>보낸 친구 요청이 없습니다.</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};