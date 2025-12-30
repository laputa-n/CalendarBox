// src/components/ScheduleModal.js
import React, { useState, useEffect } from 'react';
import { Search, Plus, Trash2 } from 'lucide-react';
import { useSchedules } from '../../contexts/ScheduleContext';
import { ApiService } from '../../services/apiService';
import { localInputToISO } from '../../utils/datetime';
import { validateSchedulePayload } from '../../utils/scheduleValidator';
import { useAttachments } from '../../hooks/useAttachments';
import { buildRecurrencePayload } from '../../utils/recurrenceBuilder';
import { COLOR_TO_THEME } from '../../utils/colorUtils';
// ✅ 생성 전용 모달 (첨부/지출은 수정 모달에서 처리)
export default function ScheduleModal({ isOpen, onClose, selectedDate }) {
  const { createSchedule } = useSchedules();
  const {
    imageQueue, fileQueue, handleSelectFiles, uploadFiles,
  } = useAttachments();

  
  // ====== 폼 상태 ======
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    startDateTime: '',
    endDateTime: '',
    color: '#3b82f6',
    places: [],             
   recurrence: {
    freq: '',       // 기본값 설정
    intervalCount: 1,     // 기본값 설정
    byDay: [],            // 기본값 설정
    until: ''             // 기본값 설정
  },        
    todos: [],              
    reminders: [],          
    links: [],
  });

 // 생성 모드 전용 로컬 입력
  const [newTodo, setNewTodo] = useState('');
 
  // ====== 지출 & 첨부파일 관련 상태 ======
  const [expenseName, setExpenseName] = useState('');
  const [expenseAmount, setExpenseAmount] = useState('');
  const [expenseReceiptFile, setExpenseReceiptFile] = useState(null);
  const [exceptionDates, setExceptionDates] = useState([]);

  // ====== 영수증 ======
  const handleReceiptUpload = (e) => {
    const file = e.target.files[0];
    if (file) setExpenseReceiptFile(file);
  };
// ====== URL 추가 ======
const handleAddLink = () => {
  const url = prompt('URL을 입력하세요:');
  const label = prompt('URL 설명을 입력하세요 (없으면 엔터):');
  if (url) {
    const newLink = { url, label: label || url };
    setFormData(prev => ({
      ...prev,
      links: [...prev.links, newLink]
    }));
  }
};

const handleRecurrenceChange = (e) => {
  const { name, value, checked } = e.target;

  setFormData(prev => {
    const next = { ...prev.recurrence };

   if (name === 'freq') {
  if (!value) {
    // ✅ 반복 없음 → 완전 초기화
    return {
      ...prev,
      recurrence: {
        freq: '',
        intervalCount: 1,
        byDay: [],
        until: '',
      },
    };
  }

  next.freq = value;
}
else if (name === 'intervalCount') {
      next.intervalCount = Number(value) || 1;
    }
    else if (name === 'byDay') {
      if (checked) {
        next.byDay = [...next.byDay, value];
      } else {
        next.byDay = next.byDay.filter(d => d !== value);
      }
    }
    else if (name === 'until') {
      next.until = value;
    }

    return {
      ...prev,
      recurrence: next,
    };

  });
};


const handleSubmit = async (e) => {
  e.preventDefault();

  const startAtISO = localInputToISO(formData.startDateTime);
  const endAtISO   = localInputToISO(formData.endDateTime);

let recurrencePayload = null;

try {
  recurrencePayload = buildRecurrencePayload(formData.recurrence);
} catch (e) {
  alert(e.message);
  return;
}

  try {
    const payload = {
      title: formData.title ?? '',
      memo: formData.description ?? '',
      startAt: localInputToISO(formData.startDateTime),
      endAt: localInputToISO(formData.endDateTime),
      todos: (formData.todos || []).map((t, i) => ({
        content: t.content ?? '',
        isDone: !!t.isDone,
        orderNo: i + 1,
      })),
      reminders: Array.isArray(formData.reminders)
        ? formData.reminders.map(r =>
            (typeof r === 'object' && Number.isFinite(r.minutesBefore))
              ? { minutesBefore: r.minutesBefore }
              : null
          ).filter(Boolean)
        : [],
      color: formData.color || '#3b82f6',
      places: [], // 예시에서는 비워둠
      links: formData.links,
      ...(recurrencePayload ? { recurrence: recurrencePayload } : {})
    };
    const errs = validateSchedulePayload ? validateSchedulePayload(payload) : [];
    if (errs.length) {
      console.warn('[Schedule] payload validation warnings:', errs);
    }

      // 1️⃣ 일정 생성
      const res = await createSchedule(payload);
      const newId = extractScheduleId(res);
      if (!newId) throw new Error('일정 생성 응답에 id가 없습니다.');

// 2️⃣ recurrenceId 조회
let recurrenceId = null;
try {
  const recRes = await ApiService.getRecurrences(newId); 
  recurrenceId = recRes?.data?.[0]?.recurrenceId ?? null;
} catch (e) {
  console.warn("⚠ 반복 없음 또는 조회 실패:", e);
}

// 3️⃣ 예외 생성
if (recurrenceId && exceptionDates.length > 0) {
  for (const d of exceptionDates) {
    try {
      await ApiService.createRecurrenceException(newId, recurrenceId, d);
    } catch (err) {
    }
  }
}
      // 2️⃣ 첨부파일 업로드 (이미지/일반파일)
      await uploadFiles(newId);

      // 3️⃣ 지출 등록 + 영수증 첨부
      if (expenseReceiptFile) {
  console.log('[RECEIPT STEP 1] 업로드 시작:', expenseReceiptFile);

  const presign = await ApiService.getPresignedUrl(newId, expenseReceiptFile, true);
  console.log('[RECEIPT STEP 2] presign 응답:', presign);

  const { uploadId, objectKey, presignedUrl } = presign.data;

  // S3 업로드
  await fetch(presignedUrl, {
    method: 'PUT',
    headers: { 'Content-Type': expenseReceiptFile.type },
    body: expenseReceiptFile,
    mode: 'cors',
    credentials: 'omit',
  });

  // OCR 트리거 (백엔드에서 attachment + OCR 처리)
  const completeRes = await ApiService.completeUpload(uploadId, objectKey);
}

// 3-2️⃣ ✅ 수동 입력 모드: 이름/금액 입력 시
if (expenseName && expenseAmount) {
  const expenseRes = await ApiService.createExpense(newId, {
    name: expenseName,
    amount: parseInt(expenseAmount, 10),
    paidAt: new Date().toISOString(),
  });
}
      // 4️⃣ 장소 개별 등록
      if (Array.isArray(formData.places) && formData.places.length) {
        for (const place of formData.places) {
          try {
            await ApiService.addSchedulePlace(newId, {
              mode: place.mode || 'PROVIDER',
              provider: place.provider || 'NAVER',
              providerPlaceKey: place.providerPlaceKey || '',
              title: place.title || place.name || '',
              category: place.category || '',
              address: place.address || '',
              roadAddress: place.roadAddress || '',
              link: place.link || '',
              lat: Number(place.lat),
              lng: Number(place.lng),
            });
          } catch (e2) {
          }
        }
      }
      alert('✅ 일정 + 첨부 + 지출 등록 완료!');
      onClose && onClose();
    } catch (error) {
      alert(error?.message || '일정 저장 중 오류가 발생했습니다.');
    }
  };

  // ====== 최초 기본값 (selectedDate 기반) ======
  useEffect(() => {
    if (!isOpen) return;
    if (selectedDate) {
      setFormData(prev => ({
        ...prev,
        title: '',
        description: '',
        startDateTime: `${selectedDate}T09:00`,
        endDateTime:   `${selectedDate}T10:00`,
        color: '#3b82f6',
        places: [],
        recurrence: { freq: '', intervalCount: 1, byDay: [], until: '' },
        todos: [],
        reminders: [],
      }));
      setNewTodo('');
    }
  }, [isOpen, selectedDate]);

  // ====== 리마인더 변환 헬퍼 ======
  const reminderSelectToMinutes = (v) => {
    switch (v) {
      case '5m': return 5;
      case '30m': return 30;
      case '1h': return 60;
      case '1d': return 1440;
      default: return null;
    }
  };

  const handleReminderChange = (e) => {
    const value = e.target.value;
    if (value === 'none') {
      setFormData(prev => ({ ...prev, reminders: [] }));
    } else {
      const minutes = reminderSelectToMinutes(value);
      setFormData(prev => ({ ...prev, reminders: [{ minutesBefore: minutes }] }));
    }
  };

  // ====== 투두(생성 모드 로컬) ======
  const handleAddTodo = () => {
    const text = newTodo.trim();
    if (!text) return;
    setFormData(prev => ({
      ...prev,
      todos: [...prev.todos, { content: text, isDone: false }],
    }));
    setNewTodo('');
  };

  const handleEditTodo = (index) => {
    const current = formData.todos[index];
    if (!current) return;
    const next = prompt('수정할 내용을 입력하세요:', current.content || '');
    if (next == null) return;
    setFormData(prev => {
      const arr = [...prev.todos];
      arr[index] = { ...arr[index], content: next };
      return { ...prev, todos: arr };
    });
  };

  const handleToggleTodo = (index) => {
    setFormData(prev => {
      const arr = [...prev.todos];
      arr[index] = { ...arr[index], isDone: !arr[index].isDone };
      return { ...prev, todos: arr };
    });
  };

  const handleRemoveTodo = (index) => {
    if (!window.confirm('이 투두를 삭제할까요?')) return;
    setFormData(prev => ({
      ...prev,
      todos: prev.todos.filter((_, i) => i !== index),
    }));
  };

  const handleReorderTodo = (direction, index) => {
    const target = direction === 'up' ? index - 1 : index + 1;
    if (target < 0 || target >= formData.todos.length) return;
    const arr = [...formData.todos];
    const tmp = arr[index];
    arr[index] = arr[target];
    arr[target] = tmp;
    setFormData(prev => ({ ...prev, todos: arr }));
  };


  
  // ====== 장소(생성 모드 로컬) ======
  const handleAddPlace = async () => {
    const query = prompt('검색할 장소명을 입력하세요.');
    if (!query) return;

    try {
      const res = await ApiService.searchPlaces(query);
      const list = res?.data?.content || res?.content || res?.data || [];
      if (!Array.isArray(list) || list.length === 0) {
        alert('검색 결과가 없습니다.');
        return;
      }

      const pick = prompt(
        list.map((p, i) => `${i + 1}. ${p.title} (${p.category || '-'})`).join('\n')
      );
      const idx = parseInt(pick, 10) - 1;
      if (isNaN(idx) || idx < 0 || idx >= list.length) return;

      const chosen = list[idx];
      const newPlace = {
        mode: 'PROVIDER',
        provider: chosen.provider || 'NAVER',
        providerPlaceKey: chosen.providerPlaceKey || '',
        title: chosen.title,
        category: chosen.category || '',
        description: chosen.description || '',
        address: chosen.address || '',
        roadAddress: chosen.roadAddress || '',
        link: chosen.link || '',
        lat: Number(chosen.lat),
        lng: Number(chosen.lng),
        name: chosen.title, // UI 표시용
      };

      setFormData(prev => ({ ...prev, places: [...prev.places, newPlace] }));
    } catch (err) {
      alert('장소 검색 중 오류가 발생했습니다.');
    }
  };

  const handleRemovePlace = (index) => {
    const target = formData.places[index];
    if (!target) return;
    if (!window.confirm(`${target.name || target.title} 장소를 삭제할까요?`)) return;
    setFormData(prev => ({
      ...prev,
      places: prev.places.filter((_, i) => i !== index),
    }));
  };

  const handleReorderPlaces = (direction, index) => {
    const target = direction === 'up' ? index - 1 : index + 1;
    if (target < 0 || target >= formData.places.length) return;
    const arr = [...formData.places];
    const tmp = arr[index];
    arr[index] = arr[target];
    arr[target] = tmp;
    setFormData(prev => ({ ...prev, places: arr }));
  };

  // ====== 제출(생성) ======
  const extractScheduleId = (res) => {
    if (!res) return undefined;
    if (res?.data?.id) return res.data.id;
    if (res?.id) return res.id;
    if (res?.data?.scheduleId) return res.data.scheduleId;
    if (res?.scheduleId) return res.scheduleId;
    return undefined;
  };

  if (!isOpen) return null;

  // ====== 렌더 ======
  return (
    <div style={overlayStyle}>
      <div style={modalStyle}>
        <h2 style={{ marginBottom: '1rem' }}>새 일정 추가</h2>

        <form onSubmit={handleSubmit}>
          {/* 제목 */}
          <input
            type="text"
            placeholder="일정 제목"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            required
            style={inputStyle}
          />

          {/* 시작 / 종료 */}
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

             {/* 장소 (생성 중 로컬로만 관리) */}
          <div style={sectionStyle}>
            <label style={labelStyle}>📍 장소</label>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="button" onClick={handleAddPlace} style={subButton} title="장소 추가">
                <Search size={16} />
              </button>
              <div style={{ flex: 1 }}>
                {formData.places.length > 0 ? (
                  formData.places.map((p, i) => (
                    <div
                      key={`${p.providerPlaceKey ?? p.title}-${i}`}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        background: '#f9fafb',
                        borderRadius: '0.5rem',
                        padding: '0.25rem 0.5rem',
                        marginBottom: '0.25rem',
                      }}
                    >
                      <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {p.name || p.title}
                      </span>
                      <div style={{ display: 'flex', gap: '0.25rem' }}>
                        <button type="button" onClick={() => handleReorderPlaces('up', i)} style={iconButton} title="위로 이동">↑</button>
                        <button type="button" onClick={() => handleReorderPlaces('down', i)} style={iconButton} title="아래로 이동">↓</button>
                        <button type="button" onClick={() => handleRemovePlace(i)} style={iconButton} title="삭제">
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  ))
                ) : (
                  <p style={{ color: '#9ca3af', fontSize: '0.875rem' }}>장소 없음</p>
                )}
              </div>
            </div>
          </div>

           {/* 투두 (생성 중 로컬로만 관리) */}
          <div style={sectionStyle}>
            <label style={labelStyle}>🧾 투두리스트</label>
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <input
                type="text"
                placeholder="할 일을 입력하세요"
                value={newTodo}
                onChange={(e) => setNewTodo(e.target.value)}
                style={{ ...inputStyle, flex: 1 }}
              />
              <button type="button" onClick={handleAddTodo} style={subButton}>
                <Plus size={16} />
              </button>
            </div>

            <ul style={{ margin: 0, paddingLeft: '1rem' }}>
              {formData.todos.map((todo, index) => (
                <li
                  key={index}
                  style={{
                    marginBottom: '0.25rem',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    listStyle: 'none',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center' }}>
                    <input
                      type="checkbox"
                      checked={!!todo.isDone}
                      onChange={() => handleToggleTodo(index)}
                      style={{ marginRight: '0.5rem' }}
                    />
                    <span
                      style={{
                        textDecoration: todo.isDone ? 'line-through' : 'none',
                        color: todo.isDone ? '#9ca3af' : '#111',
                        cursor: 'pointer',
                        maxWidth: 260,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                      onClick={() => handleEditTodo(index)}
                      title={todo.content}
                    >
                      {todo.content}
                    </span>
                  </div>

                  <div style={{ display: 'flex', gap: '0.25rem' }}>
                    <button type="button" onClick={() => handleReorderTodo('up', index)} style={iconButton} title="위로 이동">↑</button>
                    <button type="button" onClick={() => handleReorderTodo('down', index)} style={iconButton} title="아래로 이동">↓</button>
                    <button type="button" onClick={() => handleRemoveTodo(index)} style={{ ...iconButton, color: '#ef4444' }} title="삭제">
                      <Trash2 size={14} />
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </div>
          

          {/* 반복 설정 */}
<div style={sectionStyle}>
  <label style={labelStyle}>🔁 반복 주기</label>
  <select
  name="freq"
  value={formData.recurrence.freq || ''}  // ✅ 핵심
  onChange={handleRecurrenceChange}
  style={inputStyle}
>
    <option value="">없음</option>
    <option value="DAILY">매일</option>
    <option value="WEEKLY">매주</option>
    <option value="MONTHLY">매월</option>
  </select>

  {/* 반복 간격 */}
  <label style={labelStyle}>간격</label>
  <input
    type="number"
    name="intervalCount"
    value={formData.recurrence.intervalCount}
    onChange={handleRecurrenceChange}
    style={inputStyle}
  />

  {/* 반복 요일 */}
  <label style={labelStyle}>반복 요일</label>
<div style={{ display: 'flex', gap: '0.5rem' }}>
  {['MO', 'TU', 'WE', 'TH', 'FR', 'SA', 'SU'].map((day) => (
    <label key={day} style={{ display: 'flex', alignItems: 'center' }}>
      <input
        type="checkbox"
        name="byDay"
        value={day}
        checked={formData.recurrence.byDay.includes(day)}
        onChange={handleRecurrenceChange} // 클릭 시 handleRecurrenceChange 호출
        style={{ marginRight: '0.5rem' }}
      />
      {day}
    </label>
  ))}
</div>
  {/* 반복 종료일 */}
<label style={labelStyle}>반복 종료일</label>
<input
  type="datetime-local"
  name="until"
  value={formData.recurrence.until || ''}
  onChange={handleRecurrenceChange}
  style={inputStyle}
/>
{/* 반복 예외 날짜 선택 */}
{formData.recurrence.until && (
  <div style={sectionStyle}>
    <label style={labelStyle}>❌ 반복 예외 날짜 선택</label>
    <p style={{ fontSize: '0.8rem', color: '#6b7280', marginBottom: '0.5rem' }}>
      반복 기간 중 제외할 날짜를 선택하세요.
    </p>

    <input
      type="date"
      onChange={(e) => {
        const d = e.target.value;
        if (!d) return;

        setExceptionDates(prev =>
          prev.includes(d) ? prev : [...prev, d]
        );
      }}
      style={inputStyle}
    />

    {/* 선택된 예외 날짜 리스트 */}
    {exceptionDates.length > 0 && (
      <ul style={{ marginTop: '0.5rem' }}>
        {exceptionDates.map((d, i) => (
          <li
            key={i}
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              background: '#f9fafb',
              padding: '4px 8px',
              borderRadius: 6,
              marginBottom: 4
            }}
          >
            <span>{d}</span>
            <button
              type="button"
              onClick={() =>
                setExceptionDates(prev => prev.filter(x => x !== d))
              }
              style={{ ...iconButton, color: '#ef4444' }}
            >
              삭제
            </button>
          </li>
        ))}
      </ul>
    )}
  </div>
)}
</div>

          {/* 리마인더 */}
          <div style={sectionStyle}>
            <label style={labelStyle}>⏰ 리마인더</label>
            <select
              value={formData.reminders?.[0]?.minutesBefore ? minutesToSelect(formData.reminders[0].minutesBefore) : 'none'}
              onChange={handleReminderChange}
              style={inputStyle}
            >
              <option value="none">없음</option>
              <option value="5m">5분 전</option>
              <option value="30m">30분 전</option>
              <option value="1h">1시간 전</option>
              <option value="1d">하루 전</option>
            </select>
          </div>

          {/* URL 링크 추가 (텍스트 입력 필드로 수정) */}
        <div style={sectionStyle}>
          <label style={labelStyle}>🌐 링크</label>
          <div>
            <input
              type="text"
              placeholder="URL을 입력하세요"
              onChange={(e) => handleAddLink(e.target.value)}
              style={inputStyle}
            />
            <button
              type="button"
              onClick={handleAddLink}
              style={subButton}
            >
              링크 추가
            </button>
          </div>
          {formData.links.length > 0 && (
            <ul>
              {formData.links.map((link, index) => (
                <li key={index} style={{ marginBottom: '0.25rem' }}>
                  <span>{link.label} ({link.url})</span>
                </li>
              ))}
            </ul>
          )}
        </div>

          {/* 색상 */}
          <div style={sectionStyle}>
            <label style={labelStyle}>🎨 색상</label>
            <input
              type="color"
              value={formData.color}
              onChange={(e) => setFormData({ ...formData, color: e.target.value })}
              style={{ width: '100%', height: '2rem', border: 'none' }}
            />
          </div>

       
          {/* 💰 지출 등록 */}
<div style={sectionStyle}>
  <label style={labelStyle}>💰 지출 등록</label>
  <input
    type="text"
    placeholder="지출명"
    value={expenseName}
    onChange={(e) => setExpenseName(e.target.value)}
    style={inputStyle}
  />
  <input
    type="number"
    placeholder="금액"
    value={expenseAmount}
    onChange={(e) => setExpenseAmount(e.target.value)}
    style={inputStyle}
  />
  <label style={labelStyle}>📷 영수증 첨부</label>
  <input type="file" onChange={handleReceiptUpload} style={inputStyle} />
</div>

{/* 📷 이미지 첨부 */}
          <div style={sectionStyle}>
            <label style={labelStyle}>📷 이미지 첨부</label>
            <input type="file" accept="image/*" multiple onChange={handleSelectFiles} style={inputStyle} />
            {imageQueue.map((f, i) => <div key={i}>{f.name}</div>)}
          </div>

          {/* 📎 일반 파일 첨부 */}
          <div style={sectionStyle}>
            <label style={labelStyle}>📎 일반 파일 첨부</label>
            <input type="file" multiple onChange={handleSelectFiles} style={inputStyle} />
            {fileQueue.map((f, i) => <div key={i}>{f.name}</div>)}
          </div>
        
          {/* 설명 */}
          <textarea
            placeholder="메모 / 상세 내용"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            rows={3}
            style={{ ...inputStyle, height: '80px' }}
          />

          {/* 버튼 */}
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1rem' }}>
            <button type="button" onClick={onClose} style={cancelButton}>취소</button>
            <button type="submit" style={saveButton}>저장</button>
          </div>
        </form>
      </div>
    </div>
  );
}

/* ====== 내부 유틸 ====== */
function minutesToSelect(mins) {
  switch (mins) {
    case 5: return '5m';
    case 30: return '30m';
    case 60: return '1h';
    case 1440: return '1d';
    default: return 'none';
  }
}

/* ====== 스타일 ====== */
const overlayStyle = {
  position: 'fixed',
  top: 0,
  left: 0,
  width: '100%',
  height: '100%',
  backgroundColor: 'rgba(0,0,0,0.5)',
  display: 'flex',
  justifyContent: 'center',
  alignItems: 'center',
  zIndex: 1000,
};

const modalStyle = {
  backgroundColor: '#fff',
  padding: '2rem',
  borderRadius: '10px',
  width: '480px',
  maxWidth: '90%',
  maxHeight: '90vh',
  overflowY: 'auto',
};

const inputStyle = {
  width: '100%',
  marginBottom: '0.75rem',
  padding: '0.5rem',
  borderRadius: '0.5rem',
  border: '1px solid #d1d5db',
  fontSize: '0.875rem',
};

const sectionStyle = { marginBottom: '1rem' };
const labelStyle = { fontWeight: 500, fontSize: '0.875rem', marginBottom: '0.25rem', display: 'block' };

const subButton = {
  padding: '0.5rem',
  borderRadius: '0.5rem',
  background: '#e5e7eb',
  border: 'none',
  cursor: 'pointer',
};

const iconButton = {
  background: 'transparent',
  border: 'none',
  color: '#6b7280',
  cursor: 'pointer',
};

const saveButton = {
  backgroundColor: '#2563eb',
  color: '#fff',
  padding: '0.5rem 1.25rem',
  borderRadius: '0.5rem',
  border: 'none',
  cursor: 'pointer',
};

const cancelButton = {
  backgroundColor: '#e5e7eb',
  color: '#111',
  padding: '0.5rem 1.25rem',
  borderRadius: '0.5rem',
  border: 'none',
};
