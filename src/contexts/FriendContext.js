import React, { createContext, useContext, useState, useEffect } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useError } from './ErrorContext';

const FriendContext = createContext();

export const FriendProvider = ({ children }) => {
  const [friendships, setFriendships] = useState([]);
  const [sentRequests, setSentRequests] = useState([]);
  const [loading, setLoading] = useState(false);
  
  const { user , isAuthenticated } = useAuth();
  const { showError } = useError();

useEffect(() => {
  if (isAuthenticated && user) {
    fetchFriendships(); // 또는 fetchNotifications()
  }
}, [isAuthenticated, user]);

  const fetchFriendships = async () => {
    if (!user) return;
    
   try {
    setLoading(true);
    const response = await ApiService.getFriendships();
    
    // 안전한 데이터 추출
    const friendshipData = response?.data || response || [];
    setFriendships(Array.isArray(friendshipData) ? friendshipData : []);
    
  } catch (error) {
    console.error('Failed to fetch friendships:', error);
    
    // 에러 발생시 빈 배열로 설정 (mock 데이터 제거)
    setFriendships([]);
    
    // 인증 에러가 아닌 경우에만 에러 메시지 표시
    if (!error.message.includes('인증이 만료')) {
      showError('친구 목록 조회에 실패했습니다.');
    }
  } finally {
    setLoading(false);
  }
};
  const sendFriendRequest = async (friendEmail) => {
    try {
      setLoading(true);
      const response = await ApiService.sendFriendRequest(friendEmail);
      await fetchFriendships();
      return response;
    } catch (error) {
      showError(error.message || '친구 요청에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const acceptFriendship = async (friendshipId) => {
    try {
      setLoading(true);
      await ApiService.acceptFriendship(friendshipId);
      await fetchFriendships();
    } catch (error) {
      showError(error.message || '친구 승인에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const rejectFriendship = async (friendshipId) => {
    try {
      setLoading(true);
      await ApiService.rejectFriendship(friendshipId);
      await fetchFriendships();
    } catch (error) {
      showError(error.message || '친구 거절에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const removeFriend = async (friendshipId) => {
    try {
      setLoading(true);
      await ApiService.removeFriend(friendshipId);
      await fetchFriendships();
    } catch (error) {
      showError(error.message || '친구 삭제에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 친구 상태별 필터링
  const pendingFriendships = friendships.filter(f => f.status === 'pending');
  const acceptedFriendships = friendships.filter(f => f.status === 'accepted');
  const blockedFriendships = friendships.filter(f => f.status === 'blocked');

  const contextValue = {
    friendships,
    sentRequests,
    pendingFriendships,
    acceptedFriendships,
    blockedFriendships,
    loading,
    sendFriendRequest,
    acceptFriendship,
    rejectFriendship,
    removeFriend,
    refreshFriendships: fetchFriendships
  };

  return (
    <FriendContext.Provider value={contextValue}>
      {children}
    </FriendContext.Provider>
  );
};

export const useFriends = () => {
  const context = useContext(FriendContext);
  if (!context) {
    throw new Error('useFriends must be used within a FriendProvider');
  }
  return context;
};
