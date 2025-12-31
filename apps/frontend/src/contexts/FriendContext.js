//FriendContext.js
import React, { createContext, useContext, useMemo , useState, useEffect } from 'react';
import { ApiService } from '../services/apiService';
import { useAuth } from './AuthContext';
import { useError } from './ErrorContext';

const FriendContext = createContext();

export const FriendProvider = ({ children }) => {
  // 받은 친구 요청 (inbox)
  const [receivedRequests, setReceivedRequests] = useState({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    loading: false
  });
  
  // 보낸 친구 요청
  const [sentRequests, setSentRequests] = useState({
    content: [],
    page: 0,
    size: 10,
    totalElements: 0,
    totalPages: 0,
    loading: false
  });
  
  // 검색 결과
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  
  const { user, isAuthenticated } = useAuth();
  const { showError } = useError();
  // ===== 회원(Member) 검색 =====
const [memberSearchResults, setMemberSearchResults] = useState([]);
const [memberSearchLoading, setMemberSearchLoading] = useState(false);


  useEffect(() => {
    if (isAuthenticated && user) {
      fetchReceivedRequests();
      fetchSentRequests();
    }
  }, []);

  // ===== 회원 검색 (이름 / 이메일 / 전화번호) =====
const searchMembers = async (query, page = 0, size = 20) => {
  if (!query.trim()) {
    setMemberSearchResults([]);
    return;
  }

  try {
    setMemberSearchLoading(true);

    const response = await ApiService.searchMembers(query, page, size);

    setMemberSearchResults(response.data?.content || []);
  } catch (error) {
    console.error('Failed to search members:', error);
    setMemberSearchResults([]);
    showError('회원 검색에 실패했습니다.');
  } finally {
    setMemberSearchLoading(false);
  }
};


  // ===== 받은 친구 요청 조회 =====
  const fetchReceivedRequests = async (page = 1, size = 10) => {
    if (!user) return;
    
    try {
      setReceivedRequests(prev => ({ ...prev, loading: true }));
      
      const response = await ApiService.getReceivedFriendRequests(page, size);
      
      setReceivedRequests({
        content: response.data?.content || [],
        page: (response.data?.page || 0) + 1,
        size: response.data?.size || 10,
        totalElements: response.data?.totalElements || 0,
        totalPages: response.data?.totalPages || 0,
        loading: false
      });
      
    } catch (error) {
      console.error('Failed to fetch received requests:', error);
      setReceivedRequests(prev => ({ ...prev, content: [], loading: false }));
      
      if (!error.message.includes('인증이 만료')) {
        showError('받은 친구 요청 조회에 실패했습니다.');
      }
    }
  };

  // ===== 보낸 친구 요청 조회 =====
  const fetchSentRequests = async (page = 1, size = 10) => {
    if (!user) return;
    
    try {
      setSentRequests(prev => ({ ...prev, loading: true }));
      
      const response = await ApiService.getSentFriendRequests(page, size);
      
      setSentRequests({
        content: response.data?.content || [],
        page: (response.data?.page || 0) + 1,
        size: response.data?.size || 10,
        totalElements: response.data?.totalElements || 0,
        totalPages: response.data?.totalPages || 0,
        loading: false
      });
      
    } catch (error) {
      console.error('Failed to fetch sent requests:', error);
      setSentRequests(prev => ({ ...prev, content: [], loading: false }));
      
      if (!error.message.includes('인증이 만료')) {
        showError('보낸 친구 요청 조회에 실패했습니다.');
      }
    }
  };

  // ===== 사용자 검색 =====
  const searchUsers = async (query) => {
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }
    
    try {
      setSearchLoading(true);
      const response = await ApiService.searchUsers(query);
      
      setSearchResults(response.data?.content || []);
      
    } catch (error) {
      console.error('Failed to search users:', error);
      setSearchResults([]);
      showError('사용자 검색에 실패했습니다.');
    } finally {
      setSearchLoading(false);
    }
  };

  // ===== 친구 요청 보내기 =====
  const sendFriendRequest = async (query) => {
    try {
      setLoading(true);
      await ApiService.sendFriendRequest(query);
      
      await fetchSentRequests();
      
    } catch (error) {
      console.error('Failed to send friend request:', error);
      showError(error.message || '친구 요청에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // ===== 친구 요청 수락 =====
  const acceptFriendRequest = async (friendshipId) => {
    try {
      setLoading(true);
      await ApiService.acceptFriendRequest(friendshipId);
      
      await Promise.all([
        fetchReceivedRequests(),
        fetchSentRequests()
      ]);
      
    } catch (error) {
      console.error('Failed to accept friend request:', error);
      showError(error.message || '친구 요청 수락에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // ===== 친구 요청 거절 =====
  const rejectFriendRequest = async (friendshipId) => {
    try {
      setLoading(true);
      await ApiService.rejectFriendRequest(friendshipId);
      
      await fetchReceivedRequests();
      
    } catch (error) {
      console.error('Failed to reject friend request:', error);
      showError(error.message || '친구 요청 거절에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // ===== 친구 삭제 =====
  const removeFriend = async (friendshipId) => {
    try {
      setLoading(true);
      await ApiService.removeFriend(friendshipId);
      
      await Promise.all([
        fetchReceivedRequests(),
        fetchSentRequests()
      ]);
      
    } catch (error) {
      console.error('Failed to remove friend:', error);
      showError(error.message || '친구 삭제에 실패했습니다.');
      throw error;
    } finally {
      setLoading(false);
    }
  };

  // 수락된 친구 목록 계산 (필터링)
  const acceptedFriendships = useMemo(() => [
  ...receivedRequests.content.filter(f => f.status === 'ACCEPTED'),
  ...sentRequests.content.filter(f => f.status === 'ACCEPTED')
], [receivedRequests.content, sentRequests.content]);

const contextValue = {
  // 상태
  receivedRequests,
  sentRequests,
  acceptedFriendships,

  searchResults,        // (기존 – 혹시 쓰면 유지)
  searchLoading,
  memberSearchResults, // ✅ 추가
  memberSearchLoading, // ✅ 추가

  loading,

  // 메서드
  sendFriendRequest,
  acceptFriendRequest,
  rejectFriendRequest,
  removeFriend,
  fetchReceivedRequests,
  fetchSentRequests,

  searchUsers,   // (기존)
  searchMembers, // ✅ 추가
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