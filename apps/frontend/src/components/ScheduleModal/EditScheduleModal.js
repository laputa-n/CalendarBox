// src/components/EditScheduleModal.js
import React, { useState, useEffect, useCallback } from 'react';
import { Search, Plus, Trash2 } from 'lucide-react';
import { useSchedules } from '../../contexts/ScheduleContext';
import { ApiService } from '../../services/apiService';
import { toLocalInputValue, localInputToISO } from '../../utils/datetime';
import { useAttachments } from '../../hooks/useAttachments';
import ExpenseModal from '../ExpenseModal';

/* ====== 스타일 ====== */
const overlayStyle = {
  position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
  backgroundColor: 'rgba(0,0,0,0.5)',
  display: 'flex', justifyContent: 'center', alignItems: 'center',
  zIndex: 1000,
};

const modalStyle = {
  backgroundColor: '#fff',
  padding: '2rem',
  borderRadius: '10px',
  width: '520px',
  maxHeight: '90vh',
  overflowY: 'auto',
};

const sectionStyle = { marginBottom: '1rem' };
const labelStyle = { fontWeight: 500, fontSize: '0.875rem', marginBottom: '0.25rem', display: 'block' };
const inputStyle = {
  width: '100%', marginBottom: '0.75rem', padding: '0.5rem',
  borderRadius: '0.5rem', border: '1px solid #d1d5db', fontSize: '0.875rem',
};
const subButton = { padding: '0.5rem', borderRadius: '0.5rem', background: '#e5e7eb', border: 'none', cursor: 'pointer' };
const iconButton = { background: 'transparent', border: 'none', color: '#6b7280', cursor: 'pointer' };
const saveButton = { backgroundColor: '#2563eb', color: '#fff', padding: '0.5rem 1.25rem', borderRadius: '0.5rem', border: 'none', cursor: 'pointer' };
const cancelButton = { backgroundColor: '#e5e7eb', color: '#111', padding: '0.5rem 1.25rem', borderRadius: '0.5rem', border: 'none' };
const itemRow = {
  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
  background: '#f9fafb', borderRadius: 8, padding: '6px 8px', marginBottom: 6, width: '100%', 
  overflow: 'hidden', 
};
const linkStyle = {
  overflow: 'hidden',       
  textOverflow: 'ellipsis', 
  whiteSpace: 'nowrap', 
  wordBreak: 'break-word',
};

export default function EditScheduleModal({ isOpen, onClose, eventData }) {
  const {
    updateSchedule,
    fetchScheduleDetail,
    scheduleDetail,
    scheduleDetailLoading,
    clearScheduleDetail,
  } = useSchedules();

  // ✅ 1) scheduleId는 eventData에서 먼저 확보
  const scheduleId =
    eventData?.scheduleId ??
    eventData?.id ??
    eventData?.extendedProps?.scheduleId;

  useEffect(() => {
    console.log('🆔 [Modal] scheduleId =', scheduleId);
  }, [scheduleId]);

  // ✅ 2) 모달 열릴 때마다 상세조회 강제 호출
  useEffect(() => {
    if (!isOpen) return;
    if (!scheduleId) return;

    fetchScheduleDetail(scheduleId);
  }, [isOpen, scheduleId, fetchScheduleDetail]);

  // ✅ 3) 닫힐 때 상세 초기화
  useEffect(() => {
    if (!isOpen) {
      clearScheduleDetail();
    }
  }, [isOpen, clearScheduleDetail]);
  // ========== 상태 ==========
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    startDateTime: '',
    endDateTime: '',
    color: '#3b82f6',
    recurrence: null,
  });

  const [todoPage, setTodoPage] = useState({ content: [] });
  const [placePage, setPlacePage] = useState({ content: [] });
  const [attachments, setAttachments] = useState({ images: [], files: [] });
  const [expenseModalOpen, setExpenseModalOpen] = useState(false);
  const [reminders, setReminders] = useState([]);
  // 지출 관련 상태
  const [expenseName, setExpenseName] = useState('');
  const [expenseAmount, setExpenseAmount] = useState('');
  const [expensePaidAt, setExpensePaidAt] = useState('');
  const [expenseReceiptFile, setExpenseReceiptFile] = useState(null);
  const [links, setLinks] = useState([]);
  const [recurrenceList, setRecurrenceList] = useState([]);
  const [editingRecurrence, setEditingRecurrence] = useState(null);
  const [isRecurrenceEditing, setIsRecurrenceEditing] = useState(false);
  const [exceptionList, setExceptionList] = useState([]);
  const [expense, setExpense] = useState(null);
  const [lines, setLines] = useState([]);
  const [newReminder, setNewReminder] = useState('none');
  const [placeSearchResults, setPlaceSearchResults] = useState([]);
  const [isSearchingPlace, setIsSearchingPlace] = useState(false);

// ✅ links
const loadLinks = useCallback(async () => {
  if (!scheduleId) return;

  const res = await ApiService.getScheduleLinks(scheduleId);
  console.log('🌐 loadLinks raw res:', res);

  const list = Array.isArray(res?.data?.scheduleLinkDtos)
    ? res.data.scheduleLinkDtos
    : [];
  setLinks(list);
}, [scheduleId]);

const loadTodos = useCallback(async () => {
  if (!scheduleId) return;

  const res = await ApiService.listTodos(scheduleId);
  console.log('🧾 loadTodos raw res:', res);

  const list = Array.isArray(res?.data) ? res.data : [];
  list.sort((a, b) => (a.orderNo ?? 0) - (b.orderNo ?? 0));
  setTodoPage({ content: list });
}, [scheduleId]);

  const reminderSelectToMinutes = (v) => {
  switch (v) {
    case '5m': return 5;
    case '30m': return 30;
    case '1h': return 60;
    case '1d': return 1440;
    default: return null;
  }
};

const handleAddReminder = async () => {
  const minutes = reminderSelectToMinutes(newReminder);
  if (!minutes) return;

  try {
    await ApiService.createReminder(scheduleId, minutes);

    await loadReminders(scheduleId); // 목록 갱신
    setNewReminder('none');
  } catch (error) {
    console.error('리마인더 추가 실패:', error);
    alert('리마인더 추가 실패');
  }
};

// 🔁 장소 재정렬
const handleMovePlace = async (index, direction) => {
  const list = [...placePage.content];
  const targetIndex = direction === 'up' ? index - 1 : index + 1;

  if (targetIndex < 0 || targetIndex >= list.length) return;

  // 1️⃣ 프론트 swap
  [list[index], list[targetIndex]] = [list[targetIndex], list[index]];

  // 🔥 UX 즉시 반영
  setPlacePage({ content: list });

  // 2️⃣ 서버 payload
  const positions = list.map((p, i) => ({
    schedulePlaceId: p.id ?? p.schedulePlaceId,
    position: i,
  }));

  try {
    await ApiService.reorderSchedulePlaces(scheduleId, positions);
  } catch (err) {
    alert('장소 순서 변경 실패');
    await loadPlaces(); // 롤백
  }
};
// ✏️ 장소 이름 수정
const handleEditPlace = async (p) => {
  const placeId = p.id ?? p.schedulePlaceId;
  if (!placeId) return;

  const next = prompt('장소 이름 수정', p.name || p.title);
  if (next == null || !next.trim()) return;

  try {
    await ApiService.updateSchedulePlace(
      scheduleId,
      placeId,
      next.trim()
    );
    await loadPlaces();
  } catch (err) {
    console.error('장소 이름 수정 실패:', err);
    alert('장소 이름 수정 실패');
  }
};

  const loadPlaces = useCallback(async () => {
    const res = await ApiService.listSchedulePlaces(scheduleId, 0, 20);
    console.log('📍 loadPlaces response:', res);
    const data = res?.data ?? res;
    const content = Array.isArray(data?.content) ? data.content : data;
    setPlacePage({ content });
  }, [scheduleId]);

  const loadRecurrences = useCallback(async () => {
  try {
    const res = await ApiService.getRecurrences(scheduleId);
    const list = res?.data ?? [];
     console.log("🔍 [loadRecurrences] 서버 응답 list:", list);
    setRecurrenceList(list);
   console.log("🔍 [loadRecurrences] setRecurrenceList 후 state:", list);
   if (list.length > 0) {
        setEditingRecurrence(list[0]);
      } else {
        setEditingRecurrence(null);
        setExceptionList([]); // 반복 없으면 예외도 없음
      }
    } catch (err) {
      console.error("반복 조회 실패", err);
    }
  }, [scheduleId]);

const loadExceptions = useCallback(async () => {
  if (!editingRecurrence) return;

  try {
    const res = await ApiService.getRecurrenceExceptions(
      scheduleId,
      editingRecurrence.recurrenceId
    );

    const list = res?.data ?? [];
    console.log("📂 [loadExceptions] 예외 목록:", list);

    setExceptionList(list);
  } catch (err) {
  }
}, [scheduleId, editingRecurrence]);


// ✅ reminders
const loadReminders = useCallback(async () => {
  if (!scheduleId) return;

  const res = await ApiService.listReminders(scheduleId);
  console.log('⏰ loadReminders raw res:', res);

  const list = Array.isArray(res?.data) ? res.data : [];
  setReminders(list);
}, [scheduleId]);

const updateRecurrence = async (scheduleId, recurrenceId, recurrenceData) => {
  try {
    await ApiService.updateRecurrence(scheduleId, recurrenceId, recurrenceData);
    alert("반복이 수정되었습니다.");
    await loadRecurrences(scheduleId);
  } catch (err) {
    console.error("반복 수정 실패:", err);
    alert("반복 수정 실패");
  }
};

const handleDeleteException = async (exceptionId) => {
  if (!window.confirm("이 예외 날짜를 삭제할까요?")) return;

  try {
    await ApiService.deleteRecurrenceException(
      scheduleId,
      editingRecurrence.recurrenceId,
      exceptionId
    );

    await loadExceptions();
    alert("예외 날짜 삭제 완료!");
  } catch (err) {
    console.error("예외 삭제 실패:", err);
    alert("삭제 실패!");
  }
};
useEffect(() => {
  console.log('🆔 [Modal] scheduleId =', scheduleId);
}, [scheduleId]);

  const loadAttachments = useCallback(async () => {
    const [images, files] = await Promise.all([
      ApiService.getImageAttachments(scheduleId),
      ApiService.getFileAttachments(scheduleId),
    ]);
    setAttachments({
      images: images?.data || [],
      files: files?.data || [],
    });
  }, [scheduleId]);
  
function toValidISO(dt) {
  if (!dt) return null;
  if (dt.endsWith("Z")) return dt;
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(dt)) {
    return dt + ":00Z";
  }
  return dt;
}

useEffect(() => {
  if (!isOpen) {
    clearScheduleDetail();
  }
}, [isOpen, clearScheduleDetail]);

useEffect(() => {
  if (!scheduleDetail) return;

  setFormData(prev => ({
    ...prev,
    title: scheduleDetail.title || '',
    startDateTime: toLocalInputValue(scheduleDetail.startAt),
    endDateTime: toLocalInputValue(scheduleDetail.endAt),
    color: scheduleDetail.color || '#3b82f6',
  }));
}, [scheduleDetail]);
  // ========== 초기값 ==========
useEffect(() => {
  if (!scheduleDetail?.id) return;

  const id = scheduleDetail.id;

  loadTodos(id);
  loadReminders(id);
  loadPlaces(id);
  loadAttachments(id);
  loadRecurrences(id);
  loadLinks(id);
}, [scheduleDetail?.id]);

useEffect(() => {
  if (!scheduleDetail?.memo) return;

  setFormData(prev => ({
    ...prev,
    description: scheduleDetail.memo,
  }));
}, [scheduleDetail?.memo]);


useEffect(() => {
  if (!editingRecurrence) return;
  loadExceptions();
}, [editingRecurrence, loadExceptions]);


// 리마인더 삭제
const handleDeleteReminder = async (reminderId) => {
  if (window.confirm('이 리마인더를 삭제할까요?')) {
    try {
      await ApiService.deleteReminder(scheduleId, reminderId);
      loadReminders(scheduleId); // 삭제 후 목록 다시 로드
    } catch (error) {
    }
  }
};
  // ========== 장소 ==========
const handleAddPlace = async () => {
  const query = prompt('검색할 장소명을 입력하세요.');
  if (!query) return;

  try {
    setIsSearchingPlace(true);
    const res = await ApiService.searchPlaces(query);
    const list = res?.data?.content || [];

    if (!list.length) {
      alert('검색 결과가 없습니다.');
      return;
    }

    // 🔥 검색 결과 state에 저장
    setPlaceSearchResults(list);
  } catch (err) {
    alert('장소 검색 중 오류가 발생했습니다.');
  } finally {
    setIsSearchingPlace(false);
  }
};

  const handleRemovePlace = async (p) => {
    if (!window.confirm('삭제할까요?')) return;
    await ApiService.removeSchedulePlace(scheduleId, p.id ?? p.schedulePlaceId);
    loadPlaces();
  };

// 링크 삭제
const handleDeleteLink = async (linkId) => {
  if (window.confirm('이 링크를 삭제할까요?')) {
    try {
      await ApiService.deleteScheduleLink(scheduleId, linkId);
      setLinks((prevLinks) => prevLinks.filter((link) => link.scheduleLinkId !== linkId));
    } catch (error) {
      console.error('링크 삭제 실패:', error);
    }
  }
};
  // ========== 투두 ==========
  const [editTodoInput, setEditTodoInput] = useState('');

  const handleAddTodo = async () => {
    if (!editTodoInput.trim()) return;
    await ApiService.addTodo(scheduleId, editTodoInput.trim());
    setEditTodoInput('');
    loadTodos();
  };

  const handleDeleteTodo = async (t) => {
    if (!window.confirm('삭제할까요?')) return;
    await ApiService.deleteTodo(scheduleId, t.id ?? t.scheduleTodoId);
    loadTodos();
  };

  const handleToggleTodo = async (t) => {
    await ApiService.toggleTodo(scheduleId, t.id ?? t.scheduleTodoId);
    loadTodos();
  };

  const handleRenameTodo = async (t) => {
    const next = prompt('내용 수정:', t.content);
    if (next == null) return;
    await ApiService.updateTodo(scheduleId, t.id ?? t.scheduleTodoId, next);
    loadTodos();
  };

const {
  imageQueue,
  fileQueue,
  handleSelectFiles,   
  uploadFiles,         
  clearQueues,         
} = useAttachments(scheduleId);

  const handleDownload = async (attachmentId) => {
    const res = await ApiService.getDownloadUrl(attachmentId);
    if (res && typeof res === 'string') window.open(res, '_blank');
  };

  const handleDelete = async (attachmentId) => {
    if (!window.confirm('삭제하시겠습니까?')) return;
    await ApiService.deleteAttachment(attachmentId);
    await loadAttachments();
  };

  // ========== 지출 조회 ==========
useEffect(() => {
  if (!isOpen || !scheduleId) return;

  const loadExpense = async () => {
    try {
      // 1️⃣ 지출 목록
      const res = await ApiService.listExpenses(scheduleId);

      const expenseList = res?.data?.content ?? [];
      console.log('💰 expense list:', expenseList);

      const firstExpense = expenseList[0];
      if (!firstExpense) {
        setExpense(null);
        setLines([]);
        return;
      }

      // 2️⃣ 지출 상세 (🔥 lines 포함)
      const detailRes = await ApiService.getExpenseDetail(
        scheduleId,
        firstExpense.expenseId
      );

      const detail = detailRes?.data;
      console.log('🧾 expense detail:', detail);

      setExpense(detail);
      setLines(detail.lines ?? []);
    } catch (err) {
      console.error('지출 조회 실패:', err);
    }
  };

  loadExpense();
}, [isOpen, scheduleId]);

// 🔁 투두 재정렬
const handleMoveTodo = async (index, direction) => {
  const list = [...todoPage.content];
  const targetIndex = direction === 'up' ? index - 1 : index + 1;

  // 범위 체크
  if (targetIndex < 0 || targetIndex >= list.length) return;

  // 1️⃣ 프론트 배열 swap
  [list[index], list[targetIndex]] = [list[targetIndex], list[index]];

  // 2️⃣ 서버로 보낼 orders 생성 (orderNo는 0부터)
  const orders = list.map((t, i) => ({
    todoId: t.id ?? t.scheduleTodoId,
    orderNo: i,
  }));

  try {
    // 3️⃣ API 호출
    await ApiService.reorderTodos(scheduleId, orders);

    // 4️⃣ 최신 상태 다시 로드
    await loadTodos();
  } catch (err) {
    console.error('투두 재정렬 실패:', err);
    alert('투두 순서 변경 실패');
  }
};


  const handleReceiptUpload = (e) => {
    const file = e.target.files[0];
    if (file) setExpenseReceiptFile(file);
  };

  const handleAddExpense = async () => {
    if (!expenseName || !expenseAmount) return alert('지출명/금액 입력');
    const expenseData = {
      name: expenseName,
      amount: parseInt(expenseAmount, 10),
      paidAt: expensePaidAt ? new Date(expensePaidAt).toISOString() : null,
    };
    await ApiService.createExpense(scheduleId, expenseData);

    if (expenseReceiptFile) {
      const presign = await ApiService.getPresignedUrl(scheduleId, expenseReceiptFile, true);
      const { uploadId, objectKey, presignedUrl } = presign.data;
      await fetch(presignedUrl, {
        method: 'PUT',
        headers: { 'Content-Type': expenseReceiptFile.type },
        body: expenseReceiptFile,
        mode: 'cors',
        credentials: 'omit',
      });
       const completeRes = await ApiService.completeUpload(uploadId, objectKey);
      alert('지출 등록 + 영수증 업로드 완료');
    } else {
      alert('지출 등록 완료');
    }

    setExpenseName('');
    setExpenseAmount('');
    setExpensePaidAt('');
    setExpenseReceiptFile(null);
  };

  // ========== 저장 ==========
  const handleSubmit = async (e) => {
    e.preventDefault();
    await updateSchedule(scheduleId, {
      title: formData.title,
      memo: formData.description,
      startAt: localInputToISO(formData.startDateTime),
      endAt: localInputToISO(formData.endDateTime),
      color: formData.color,
    });
    
    // ✅ 새 첨부파일 업로드(선택된 경우만)
  if (imageQueue.length > 0 || fileQueue.length > 0) {
    await uploadFiles(scheduleId);
        await loadAttachments(); // 업로드 반영
  }

  alert('저장 완료');
  onClose();
  };

if (!isOpen) return null;

// ========== 렌더 ==========
return (
  <div style={overlayStyle}>
    <div style={modalStyle}>
      {!scheduleId ? (
        <div style={{ textAlign: 'center', padding: '1rem' }}>
          <p style={{ color: '#ef4444', marginBottom: '1rem' }}>
            ⚠️ 일정 정보가 올바르지 않습니다.
          </p>
          <button type="button" onClick={onClose} style={cancelButton}>
            닫기
          </button>
        </div>
      ) : (
        <>
          <h2 style={{ marginBottom: '1rem' }}>일정 수정</h2>

          <form onSubmit={handleSubmit}>
            {/* 기본 정보 */}
            <input
              type="text"
              placeholder="제목"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              style={inputStyle}
            />
            <input
              type="datetime-local"
              value={formData.startDateTime}
              onChange={(e) => setFormData({ ...formData, startDateTime: e.target.value })}
              style={inputStyle}
            />
            <input
              type="datetime-local"
              value={formData.endDateTime}
              onChange={(e) => setFormData({ ...formData, endDateTime: e.target.value })}
              style={inputStyle}
            />
          

            {/* 장소 */}
              <label style={labelStyle}>📍 장소</label>
              <button type="button" onClick={handleAddPlace} style={subButton}>
                + 장소
              </button>
              {placePage.content.map((p, index) => (
  <div key={p.id ?? p.schedulePlaceId} style={itemRow}>
    <span>{p.name || p.title}</span>

    <div style={{ display: 'flex', gap: 4 }}>
      <button
        type="button"
        onClick={() => handleMovePlace(index, 'up')}
        style={iconButton}
      >
        ↑
      </button>
      <button
        type="button"
        onClick={() => handleMovePlace(index, 'down')}
        style={iconButton}
      >
        ↓
      </button>
      <button
        type="button"
        onClick={() => handleEditPlace(p)}
        style={iconButton}
      >
        수정
      </button>
      <button
        type="button"
        onClick={() => handleRemovePlace(p)}
        style={{ ...iconButton, color: '#ef4444' }}
      >
        삭제
      </button>
    </div>
  </div>
))}

{/* 🔍 장소 검색 결과 */}
{placeSearchResults.length > 0 && (
  <div style={{ marginTop: 8 }}>
    <p style={{ fontSize: '0.8rem', color: '#6b7280' }}>
      장소를 클릭해서 추가하세요
    </p>

    {placeSearchResults.map((p) => (
      <div
        key={p.providerPlaceKey}
        onClick={async () => {
          try {
            await ApiService.addSchedulePlace(scheduleId, {
              mode: 'PROVIDER',
              provider: p.provider || 'NAVER',
              providerPlaceKey: p.providerPlaceKey,
              title: p.title,
              category: p.category || '',
              address: p.address || '',
              roadAddress: p.roadAddress || '',
              lat: Number(p.lat),
              lng: Number(p.lng),
            });

            // ✅ 서버 반영 후 재조회
            await loadPlaces();
          } catch (err) {
            alert('장소 추가 실패');
          } finally {
            // ✅ 검색 결과 닫기
            setPlaceSearchResults([]);
          }
        }}
        style={{
          padding: '8px',
          borderRadius: 8,
          background: '#f9fafb',
          marginBottom: 6,
          cursor: 'pointer',
          border: '1px solid #e5e7eb',
        }}
      >
        <strong>{p.title}</strong>
        <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
          {p.roadAddress || p.address}
        </div>
      </div>
    ))}
  </div>
)}



            {/* 투두 */}
            <div style={sectionStyle}>
              <label style={labelStyle}>🧾 투두</label>
              <div style={{ display: 'flex', gap: '0.5rem', marginBottom: 6 }}>
                <input
                  type="text"
                  placeholder="새 투두"
                  value={editTodoInput}
                  onChange={(e) => setEditTodoInput(e.target.value)}
                  style={{ ...inputStyle, flex: 1, marginBottom: 0 }}
                />
                <button type="button" onClick={handleAddTodo} style={subButton}>
                  <Plus size={16} />
                </button>
              </div>
              {todoPage.content.map((t, index) => (
  <div key={t.id ?? t.scheduleTodoId} style={itemRow}>
    <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <input
        type="checkbox"
        checked={!!t.isDone}
        onChange={() => handleToggleTodo(t)}
      />
      <span style={{ textDecoration: t.isDone ? 'line-through' : 'none' }}>
        {t.content}
      </span>
    </label>

    <div style={{ display: 'flex', gap: 4 }}>
      <button
        type="button"
        onClick={() => handleMoveTodo(index, 'up')}
        style={iconButton}
      >
        ↑
      </button>
      <button
        type="button"
        onClick={() => handleMoveTodo(index, 'down')}
        style={iconButton}
      >
        ↓
      </button>
      <button
        type="button"
        onClick={() => handleRenameTodo(t)}
        style={iconButton}
      >
        수정
      </button>
      <button
        type="button"
        onClick={() => handleDeleteTodo(t)}
        style={{ ...iconButton, color: '#ef4444' }}
      >
        삭제
      </button>
    </div>
  </div>
))}

            </div>
            {/* 반복 정보 */}
<div style={sectionStyle}>
  <label style={labelStyle}>🔁 반복 설정</label>

  {editingRecurrence ? (
    <>
      <div style={{ marginBottom: "8px" }}>
        <strong>반복 유형:</strong> {editingRecurrence.freq}
      </div>
      <div style={{ marginBottom: "8px" }}>
        <strong>간격:</strong> {editingRecurrence.intervalCount}
      </div>
      {editingRecurrence.byDay?.length > 0 && (
        <div style={{ marginBottom: "8px" }}>
          <strong>요일:</strong> {editingRecurrence.byDay.join(', ')}
        </div>
      )}
      {editingRecurrence.until && (
        <div style={{ marginBottom: "8px" }}>
          <strong>종료:</strong> {editingRecurrence.until}
        </div>
      )}

      {/* 수정버튼 */}
      <button
       type="button"
       style={{ ...subButton, background: "#dbeafe" }}
       onClick={() => setIsRecurrenceEditing(true)}
>
       반복 수정
      </button>
      {isRecurrenceEditing && (
  <div style={{ marginTop: '0.75rem', padding: '0.75rem', background: '#f3f4f6', borderRadius: 8 }}>

    <label style={labelStyle}>🔁 반복 주기</label>
    <select
      name="freq"
      value={editingRecurrence.freq}
      onChange={(e) =>
        setEditingRecurrence(prev => ({ ...prev, freq: e.target.value }))
      }
      style={inputStyle}
    >
      <option value="">없음</option>
      <option value="DAILY">매일</option>
      <option value="WEEKLY">매주</option>
      <option value="MONTHLY">매월</option>
    </select>

    <label style={labelStyle}>간격</label>
    <input
      type="number"
      name="intervalCount"
      value={editingRecurrence.intervalCount}
      onChange={(e) =>
        setEditingRecurrence(prev => ({ ...prev, intervalCount: Number(e.target.value) }))
      }
      style={inputStyle}
    />

    <label style={labelStyle}>반복 요일</label>
    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
      {['MO', 'TU', 'WE', 'TH', 'FR', 'SA', 'SU'].map((day) => (
        <label key={day} style={{ display: 'flex', alignItems: 'center' }}>
          <input
            type="checkbox"
            name="byDay"
            value={day}
            checked={editingRecurrence.byDay?.includes(day)}
            onChange={() => {
              setEditingRecurrence(prev => {
                const exists = prev.byDay.includes(day);
                return {
                  ...prev,
                  byDay: exists
                    ? prev.byDay.filter(d => d !== day)
                    : [...prev.byDay, day]
                };
              });
            }}
            style={{ marginRight: '0.5rem' }}
          />
          {day}
        </label>
      ))}
    </div>

    <label style={labelStyle}>반복 종료일</label>
    <input
      type="datetime-local"
      name="until"
      value={editingRecurrence.until || ''}
      onChange={(e) =>
        setEditingRecurrence(prev => ({ ...prev, until: e.target.value }))
      }
      style={inputStyle}
    />
    {/* 🔥 반복 예외 목록 UI */}
<div style={{ marginTop: '1.2rem' }}>
  <label style={{ ...labelStyle, fontWeight: 600 }}>❗ 반복 예외 날짜</label>

  {exceptionList.length === 0 ? (
    <p style={{ color: "#9ca3af" }}>예외 날짜 없음</p>
  ) : (
    exceptionList.map((ex) => (
      <div key={ex.exceptionId} style={itemRow}>
        <span>{ex.exceptionDate}</span>

        <button
          type="button"
          onClick={() => handleDeleteException(ex.exceptionId)}
          style={{ ...iconButton, color: '#ef4444' }}
        >
          <Trash2 size={16} />
        </button>
      </div>
    ))
  )}
</div>

    {/* 저장 버튼 */}
    <button
  type="button"
  style={{ ...subButton, background: '#3b82f6', color: '#fff', marginTop: 8 }}
  onClick={() => {
    if (!editingRecurrence.freq) {
  alert('반복 유형을 선택하세요.');
  return;
}
    const fixedData = {
  freq: editingRecurrence.freq,
  intervalCount: editingRecurrence.intervalCount,
  byDay: editingRecurrence.byDay?.length ? editingRecurrence.byDay : null,
  until: editingRecurrence.until
    ? toValidISO(editingRecurrence.until)
    : null,
};
    updateRecurrence(
      scheduleId,
      editingRecurrence.recurrenceId,
      fixedData
    );
    
    setIsRecurrenceEditing(false);
  }}
>
  저장
</button>
  </div>
)}

      {/* 삭제 버튼 */}
      <button
        type="button"
        style={{ ...subButton, background: "#fee2e2", color: "#b91c1c", marginLeft: "8px" }}
       onClick={async () => {
  if (!window.confirm("반복을 삭제할까요?")) return;

  // 🔥 최신값 보장
  await loadRecurrences(scheduleId);
  console.log("🧪 삭제 후 다시 불러온 recurrenceList:", recurrenceList);

  const target = editingRecurrence || recurrenceList[0];
  if (!target || !target.recurrenceId) {
    alert("반복 정보를 찾을 수 없습니다.");
    return;
  }
  await ApiService.deleteRecurrence(scheduleId, target.recurrenceId);
  await loadRecurrences(scheduleId);
  alert("반복 삭제 완료");
          console.log("🗑 삭제 요청 scheduleId:", scheduleId);
        }}
      >
        반복 삭제
      </button>
    </>
  ) : (
    <p style={{ color: "#9ca3af" }}>반복 없음</p>
  )}
  
</div>
      {/* 리마인더 UI */}
      <div style={sectionStyle}>
        <label style={labelStyle}>⏰ 리마인더</label>
        
        {/* 리마인더 목록 */}
        {reminders.length > 0 ? (
          reminders.map((reminder) => (
            <div key={reminder.scheduleReminderId} style={itemRow}>
              <span>{reminder.minutesBefore}분 전</span>
              <button
                type="button"
                onClick={() => handleDeleteReminder(reminder.scheduleReminderId)}
                style={{ ...iconButton, color: '#ef4444' }}
              >
                <Trash2 size={16} />
              </button>
            </div>
          ))
        ) : (
          <p style={{ color: '#9ca3af', fontSize: '0.875rem' }}>리마인더 없음</p>
        )}
      </div>

      {/* 🔥 리마인더 추가 */}
<div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
  <select
    value={newReminder}
    onChange={(e) => setNewReminder(e.target.value)}
    style={{ ...inputStyle, marginBottom: 0 }}
  >
    <option value="none">추가할 리마인더</option>
    <option value="5m">5분 전</option>
    <option value="30m">30분 전</option>
    <option value="1h">1시간 전</option>
    <option value="1d">하루 전</option>
  </select>

  <button
    type="button"
    onClick={handleAddReminder}
    style={{ ...subButton, background: '#dbeafe' }}
  >
    추가
  </button>
</div>


        <div style={sectionStyle}>
  <label style={labelStyle}>🌐 링크</label>
 {links.length > 0 ? (
  links.map((link, index) => (
    <div key={index} style={itemRow}>
      <a
        href={link.url}
        target="_blank"
        rel="noopener noreferrer"
        style={linkStyle}
      >
        {link.label || link.url}
      </a>
      <button
        type="button"
        onClick={() => handleDeleteLink(link.scheduleLinkId)}
        style={iconButton}
      >
        <Trash2 size={16} />
      </button>
    </div>
  ))
) : (
  <p style={{ color: '#9ca3af', fontSize: '0.875rem' }}>링크 없음</p>
)}
</div>
           
               {/* 지출 섹션 상단이나 하단 아무데나 배치 */}
        <div style={{ display: 'flex', justifyContent: 'flex-start', margin: '8px 0 12px' }}>
          <button
            type="button"
            onClick={() => setExpenseModalOpen(true)}
            style={{ padding: '8px 12px', borderRadius: 8, border: 'none', background: '#10b981', color: '#fff', fontWeight: 600, cursor: 'pointer' }}
          >
            💰 지출 관리 열기
          </button>
        </div>
        {expense && (
  <div style={sectionStyle}>
    <label style={labelStyle}>💰 지출</label>

    <div style={itemRow}>
      <strong>{expense.name}</strong>
      <span>{expense.amount?.toLocaleString()}원</span>
    </div>

    {lines.map(line => (
      <div
        key={line.expenseLineId}
        style={{ paddingLeft: 8, fontSize: '0.875rem', color: '#374151' }}
      >
        • {line.label} ({line.lineAmount.toLocaleString()}원)
      </div>
    ))}
  </div>
)}
       <ExpenseModal
  isOpen={expenseModalOpen}
  onClose={() => {
    setExpenseModalOpen(false);
    // 🔥 지출 다시 로드
    ApiService.listExpenses(scheduleId).then(res => {
      const e = res.data?.[0];
      if (!e) {
        setExpense(null);
        setLines([]);
        return;
      }
      setExpense(e);
      ApiService.listExpenseLines(e.expenseId).then(r =>
        setLines(r.data?.content ?? [])
      );
    });
  }}
  scheduleId={scheduleId}
/>

{/* 기존 첨부파일 */}
<div style={sectionStyle}>
  <label style={labelStyle}>📂 기존 첨부파일</label>

  {attachments.images.length > 0 && (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
      {attachments.images.map((img) => (
        <div key={img.id} style={{ position: 'relative', width: 80, height: 80 }}>
          <img
            src={img.thumbUrl || img.imageUrl || img.url}
            alt={img.name || img.fileName}
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'cover',
              borderRadius: 8,
              cursor: 'pointer',
            }}
            onClick={() => handleDownload(img.id)}
          />
          <button
            onClick={() => handleDelete(img.id)}
            style={{
              position: 'absolute',
              top: 2,
              right: 2,
              background: 'rgba(0,0,0,0.4)',
              color: '#fff',
              border: 'none',
              borderRadius: '50%',
              cursor: 'pointer',
            }}
          >
            ×
          </button>
        </div>
      ))}
    </div>
  )}

  {attachments.files.length > 0 && (
    <ul>
      {attachments.files.map((f) => (
        <li key={f.id}>
          <span
            style={{ color: '#2563eb', cursor: 'pointer' }}
            onClick={() => handleDownload(f.id)}
          >
            {f.name || f.fileName}
          </span>
          <button
            onClick={() => handleDelete(f.id)}
            style={{ ...iconButton, color: '#ef4444', marginLeft: 8 }}
          >
            삭제
          </button>
        </li>
      ))}
    </ul>
  )}
</div>

{/* 새 첨부파일 선택 */}
<div style={sectionStyle}>
  <label style={labelStyle}>➕ 새 이미지 첨부</label>
  <input
    type="file"
    accept="image/*"
    multiple
    onChange={handleSelectFiles}
    style={inputStyle}
  />
  {imageQueue.map((f, i) => (
    <div key={`img-q-${i}`} style={{ fontSize: '0.875rem' }}>
      • {f.name}
    </div>
  ))}
</div>

<div style={sectionStyle}>
  <label style={labelStyle}>➕ 새 일반 파일 첨부</label>
  <input
    type="file"
    multiple
    onChange={handleSelectFiles}
    style={inputStyle}
  />
  {fileQueue.map((f, i) => (
    <div key={`file-q-${i}`} style={{ fontSize: '0.875rem' }}>
      • {f.name}
    </div>
  ))}
</div>

               <textarea
              placeholder="메모"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              rows={3}
              style={{ ...inputStyle, height: '80px' }}
            />
            {/* 하단 버튼 */}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 16 }}>
              <button type="button" onClick={onClose} style={cancelButton}>
                닫기
              </button>
              <button type="submit" style={saveButton}>
                저장
              </button>

 
            </div>
          </form>
        </>
      )}
    </div>
  </div>
  
);
}