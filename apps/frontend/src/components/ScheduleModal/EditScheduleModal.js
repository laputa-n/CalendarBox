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
  const { updateSchedule } = useSchedules();
 const scheduleId = Number(eventData?.id || eventData?.scheduleId);


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
  const [recurrence, setRecurrence] = useState({
  freq: 'WEEKLY',
  intervalCount: 1,
  byDay: ['MO', 'WE', 'FR'],
  until: '', // 종료 날짜
});

const handleRecurrenceChange = (e) => {
  const { name, value } = e.target;
  setRecurrence(prev => ({
    ...prev,
    [name]: value
  }));
};




  const loadLinks = useCallback(async (scheduleId) => {
  try {
    const res = await ApiService.getScheduleLinks(scheduleId);
    console.log('링크 조회 성공:', res); // 응답 데이터 확인
     setLinks(res?.data?.scheduleLinkDtos || []);
  } catch (error) {
    console.error('링크 조회 실패:', error);
  }
}, []);
  // ========== 로드 ==========
    const loadData = useCallback(async () => {
    try {
      await Promise.all([
        loadTodos(scheduleId),
        loadPlaces(scheduleId),
        loadAttachments(scheduleId),
        loadReminders(scheduleId),
        loadLinks(scheduleId)
      ]);
    } catch (error) {
      console.error('데이터 로드 실패:', error);
    }
  }, [scheduleId, loadLinks]);

  const loadTodos = useCallback(async () => {
    const res = await ApiService.listTodos(scheduleId, 0, 50);
    const data = res?.data ?? res;
    const content = Array.isArray(data?.content) ? data.content : data;
    setTodoPage({ content });
  }, [scheduleId]);

  const loadPlaces = useCallback(async () => {
    const res = await ApiService.listSchedulePlaces(scheduleId, 0, 20);
    console.log('📍 loadPlaces response:', res);
    const data = res?.data ?? res;
    const content = Array.isArray(data?.content) ? data.content : data;
    setPlacePage({ content });
  }, [scheduleId]);

  // 리마인더 목록 조회
const loadReminders = useCallback(async (scheduleId) => {
  try {
    const res = await ApiService.listReminders(scheduleId);
    setReminders(res.data || []);
  } catch (error) {
    console.error('리마인더 조회 실패:', error);
  }
}, []);


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

  // ========== 초기값 ==========
  useEffect(() => {
    if (!isOpen || !eventData) return;
    console.log('🧩 [EditScheduleModal] eventData:', eventData);
    setFormData({
      title: eventData.title || '',
      description: eventData.description || '',
      startDateTime: toLocalInputValue(eventData.startDateTime || eventData.startAt),
      endDateTime: toLocalInputValue(eventData.endDateTime || eventData.endAt),
      color: eventData.color || '#3b82f6',
      recurrence: eventData.recurrence || null,
    });
      loadData();
  }, [isOpen, eventData, loadData]);

// 리마인더 삭제
const handleDeleteReminder = async (reminderId) => {
  if (window.confirm('이 리마인더를 삭제할까요?')) {
    try {
      await ApiService.deleteReminder(scheduleId, reminderId);
      loadReminders(scheduleId); // 삭제 후 목록 다시 로드
    } catch (error) {
      console.error('리마인더 삭제 실패:', error);
    }
  }
};
  // ========== 장소 ==========
  const handleAddPlace = async () => {
    const query = prompt('검색할 장소명을 입력하세요.');
    if (!query) return;
    const res = await ApiService.searchPlaces(query);
    const list = res?.data?.content || [];
    if (!list.length) return alert('검색 결과가 없습니다.');
    const pick = prompt(
      list.map((p, i) => `${i + 1}. ${p.title} (${p.category || '-'})`).join('\n')
    );
    const idx = parseInt(pick, 10) - 1;
    if (isNaN(idx) || idx < 0 || idx >= list.length) return;
    const chosen = list[idx];
    await ApiService.addSchedulePlace(scheduleId, {
      mode: 'PROVIDER',
      provider: 'NAVER',
      providerPlaceKey: chosen.providerPlaceKey,
      title: chosen.title,
      category: chosen.category,
      address: chosen.address,
      roadAddress: chosen.roadAddress,
      lat: Number(chosen.lat),
      lng: Number(chosen.lng),
    });
    loadPlaces();
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
  handleSelectFiles,   // onChange에 그대로 물리면 큐에 자동 분류
  uploadFiles,         // 실제 업로드 (scheduleId 전달)
  clearQueues,         // 닫을 때 초기화용(선택)
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

  // ========== 지출 ==========
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
      console.log('[RECEIPT STEP 1] 업로드 시작:', expenseReceiptFile);
      const presign = await ApiService.getPresignedUrl(scheduleId, expenseReceiptFile, true);
       console.log('[RECEIPT STEP 2] presign 응답:', presign);

      const { uploadId, objectKey, presignedUrl } = presign.data;
      await fetch(presignedUrl, {
        method: 'PUT',
        headers: { 'Content-Type': expenseReceiptFile.type },
        body: expenseReceiptFile,
      });
       console.log('[RECEIPT STEP 3] S3 업로드 완료:', { uploadId, objectKey });
       const completeRes = await ApiService.completeUpload(uploadId, objectKey);
        console.log('[RECEIPT STEP 4] completeUpload 응답:', completeRes);
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
      recurrence: formData.recurrence,
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
            <div style={sectionStyle}>
              <label style={labelStyle}>📍 장소</label>
              <button type="button" onClick={handleAddPlace} style={subButton}>
                + 장소
              </button>
              {placePage.content.map((p) => (
                <div key={p.id ?? p.schedulePlaceId} style={itemRow}>
                  <span>{p.name || p.title}</span>
                  <button
                    type="button"
                    onClick={() => handleRemovePlace(p)}
                    style={iconButton}
                  >
                    삭제
                  </button>
                </div>
              ))}
            </div>

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
              {todoPage.content.map((t) => (
                <div key={t.id ?? t.scheduleTodoId} style={itemRow}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <input
                      type="checkbox"
                      checked={!!t.isDone}
                      onChange={() => handleToggleTodo(t)}
                    />
                    <span
                      style={{ textDecoration: t.isDone ? 'line-through' : 'none' }}
                    >
                      {t.content}
                    </span>
                  </label>
                  <div style={{ display: 'flex', gap: 4 }}>
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

        <ExpenseModal
         isOpen={expenseModalOpen}
         onClose={() => setExpenseModalOpen(false)}
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