import React, { useState } from 'react';
import { Users, UserPlus, User, CheckCircle, XCircle, Trash2 } from 'lucide-react';
import { useFriends } from '../../contexts/FriendContext';
import { validateEmail } from '../../utils/validationUtils';
import { LoadingSpinner } from '../common/LoadingSpinner';

export const FriendsPage = () => {
  const { 
    pendingFriendships, 
    acceptedFriendships, 
    sendFriendRequest, 
    acceptFriendship, 
    rejectFriendship,
    removeFriend,
    loading 
  } = useFriends();
  
  const [showAddForm, setShowAddForm] = useState(false);
  const [friendEmail, setFriendEmail] = useState('');
  const [emailError, setEmailError] = useState('');

  const handleAddFriend = async (e) => {
    e.preventDefault();
    
    // 이메일 유효성 검사
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

      {/* 친구 추가 폼 */}
      {showAddForm && (
        <div style={{ ...cardStyle, marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem' }}>
            새 친구 추가
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

      {/* 친구 요청 */}
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
                    {friendship.requesterUser.name}
                  </h4>
                  <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: '0.25rem 0' }}>
                    {friendship.requesterUser.email}
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
                    {friendship.requesterUser.name}
                  </h4>
                  <p style={{ fontSize: '0.875rem', color: '#6b7280', margin: '0.25rem 0' }}>
                    {friendship.requesterUser.email}
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
    </div>
  );
};
