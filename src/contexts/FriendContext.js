// contexts/FriendContext.js
import React, { createContext, useContext, useState, useEffect } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useError } from './ErrorContext';

const FriendContext = createContext();

export const FriendProvider = ({ children }) => {
  // 기존 상태
  const [friendships, setFriendships] = useState([]);
  const [loading, setLoading] = useState(false);
  
  // 새로운 상태들 (친구 요청 관련)
  const [receivedRequests, setReceivedRequests] = useState({
    content: [],
    page: 1,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    loading: false
  });
  
  const [sentRequests, setSentRequests] = useState({
    content: [],
    page: 1,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    loading: false
  });
  
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  
  const { user, isAuthenticated } = useAuth();
  const { showError } = useError();

  useEffect(() => {
    if (isAuthenticated && user) {
      fetchFriendships();
      fetchReceivedRequests();
      fetchSentRequests();
    }
  }, [isAuthenticated, user]);

  // 기존 친구 목록 조회 (유지)
  const fetchFriendships = async () => {
    if (!user) return;
    
    try {
      setLoading(true);
      const response = await ApiService.getFriendships();
      
      const friendshipData = response?.data || response || [];
      setFriendships(Array.isArray(friendshipData) ? friendshipData : []);
      
    } catch (error) {
      console.error('Failed to fetch friendships:', error);
      setFriendships([]);
      
      if (!error.message.includes('인증이 만료')) {
        showError('친구 목록 조회에 실패했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  // 받은 친구 요청 조회 (새로운)
  const fetchReceivedRequests = async (page = 1, size = 10, status = null) => {
    if (!user) return;
    
    try {
      setReceivedRequests(prev => ({ ...prev, loading: true }));
      
      const response = await ApiService.getReceivedFriendRequests(page, size, status);
      
      if (response?.data) {
        setReceivedRequests({
          content: response.data.content || [],
          page: response.data.page || 1,
          size: response.data.size || 10,
          totalElements: response.data.totalElements || 0,
          totalPages: response.data.totalPages || 0,
          loading: false
        });
      }
      
    } catch (error) {
      console.error('Failed to fetch received requests:', error);
      setReceivedRequests(prev => ({ ...prev, loading: false }));
      
      if (!error.message.includes('인증이 만료')) {
        showError('받은 친구 요청 조회에 실패했습니다.');
      }
    }
  };

  // 보낸 친구 요청 조회 (새로운)
  const fetchSentRequests = async (page = 1, size = 10) => {
    if (!user) return;
    
    try {
      setSentRequests(prev => ({ ...prev, loading: true }));
      
      const response = await ApiService.getSentFriendRequests(page, size);
      
      if (response?.data) {
        setSentRequests({
          content: response.data.content || [],
          page: response.data.page || 1,
          size: response.data.size || 10,
          totalElements: response.data.totalElements || 0,
          totalPages: response.data.totalPages || 0,
          loading: false
        });
      }
      
    } catch (error) {
      console.error('Failed to fetch sent requests:', error);
      setSentRequests(prev => ({ ...prev, loading: false }));
      
      if (!error.message.includes('인증이 만료')) {
        showError('보낸 친구 요청 조회에 실패했습니다.');
      }
    }
  };

  // 사용자 검색 (새로운)
  const searchUsers = async (query) => {
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }
    
    try {
      setSearchLoading(true);
      const response = await ApiService.searchUsers(query);
      
      if (response?.data?.content) {
        setSearchResults(response.data.content);
      } else {
        setSearchResults([]);
      }
      
    } catch (error) {
      console.error('Failed to search users:', error);
      setSearchResults([]);
      showError('사용자 검색에 실패했습니다.');
    } finally {
      setSearchLoading(false);
    }
  };

  // 친구 요청 보내기 (ID 기반, 새로운)
  const sendFriendRequestById = async (addresseeId) => {
    try {
      setLoading(true);
      const response = await ApiService.sendFriendRequestById(addresseeId);
      
      // 성공시 보낸 요청 목록 새로고침
      await fetchSentRequests();
      
      return response;
    } catch (error) {
      showError(error.message || '친구 요청에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 기존 친구 요청 보내기 (이메일 기반, 유지)
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

  // 친구 요청 수락 (새로운 API 사용)
  const acceptFriendRequest = async (friendshipId) => {
    try {
      setLoading(true);
      await ApiService.acceptFriendRequest(friendshipId);
      
      // 받은 요청 목록과 친구 목록 새로고침
      await Promise.all([
        fetchReceivedRequests(),
        fetchFriendships()
      ]);
    } catch (error) {
      showError(error.message || '친구 요청 수락에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 친구 요청 거절 (새로운 API 사용)
  const rejectFriendRequest = async (friendshipId) => {
    try {
      setLoading(true);
      await ApiService.rejectFriendRequest(friendshipId);
      
      // 받은 요청 목록 새로고침
      await fetchReceivedRequests();
    } catch (error) {
      showError(error.message || '친구 요청 거절에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 기존 메서드들 (유지)
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

  // 기존 필터링 (유지)
  const pendingFriendships = friendships.filter(f => f.status === 'pending');
  const acceptedFriendships = friendships.filter(f => f.status === 'accepted');
  const blockedFriendships = friendships.filter(f => f.status === 'blocked');

  const contextValue = {
    // 기존 값들
    friendships,
    pendingFriendships,
    acceptedFriendships,
    blockedFriendships,
    loading,
    
    // 새로운 값들
    receivedRequests,
    sentRequests,
    searchResults,
    searchLoading,
    
    // 기존 메서드들
    sendFriendRequest,
    acceptFriendship,
    rejectFriendship,
    removeFriend,
    refreshFriendships: fetchFriendships,
    
    // 새로운 메서드들
    sendFriendRequestById,
    acceptFriendRequest,
    rejectFriendRequest,
    fetchReceivedRequests,
    fetchSentRequests,
    searchUsers
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