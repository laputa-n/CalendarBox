// src/components/ScheduleModal.js
import React, { useState, useEffect } from 'react';
import { Search, Plus, Trash2 } from 'lucide-react';
import { useSchedules } from '../../contexts/ScheduleContext';
import { ApiService } from '../../services/apiService';
import { localInputToISO } from '../../utils/datetime';
import { validateSchedulePayload } from '../../utils/scheduleValidator';
import { useAttachments } from '../../hooks/useAttachments';
import { buildRecurrencePayload } from '../../utils/recurrenceBuilder';
import ScheduleParticipantsSection from '../schedule/ScheduleParticipantsSection';
import { useCalendars } from '../../contexts/CalendarContext';

export default function ScheduleModal({ isOpen, onClose, selectedDate }) {
  const { createSchedule } = useSchedules();
  const {
    imageQueue, fileQueue, handleSelectFiles, uploadFiles,
  } = useAttachments();

  const [isMonthlyRuleOpen, setIsMonthlyRuleOpen] = useState(false);
const [monthlyOrdinal, setMonthlyOrdinal] = useState('');     // ByDay의 +/- 숫자 (예: 1, 2, -1). 빈값이면 ByMonthDay 사용
const [monthlyWeekday, setMonthlyWeekday] = useState('MO');   // 요일
const [monthlyMonthDay, setMonthlyMonthDay] = useState('');   // ByMonthDay 날짜 (1~31)
const [monthlyMode, setMonthlyMode] = useState('BYDAY'); 
const { currentCalendar } = useCalendars();
const [calendarMembers, setCalendarMembers] = useState([]);
const [friends, setFriends] = useState([]);
const [serviceUserResults, setServiceUserResults] = useState([]);
const [searchingServiceUser, setSearchingServiceUser] = useState(false);
const [myMemberId, setMyMemberId] = useState(null);

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
    byDay: [],             // 기본값 설정
    byMonthDay: '',        
    until: ''             // 기본값 설정
  },        
    todos: [],              
    reminders: [],          
    links: [],
  });
  const [newTodo, setNewTodo] = useState('');
  const [placeSearchResults, setPlaceSearchResults] = useState([]);
  const [isSearchingPlace, setIsSearchingPlace] = useState(false);
  const [invitees, setInvitees] = useState([]);
  // ====== 지출 & 첨부파일 관련 상태 ======
  const [expenseName, setExpenseName] = useState('');
  const [expenseReceiptFile, setExpenseReceiptFile] = useState(null);
  const [exceptionDates, setExceptionDates] = useState([]);
  const [expenseLines, setExpenseLines] = useState([]);

  // ====== 영수증 ======
  const handleReceiptUpload = (e) => {
    const file = e.target.files[0];
    if (file) setExpenseReceiptFile(file);
  };
const [linkInput, setLinkInput] = useState('');

const handleAddLink = () => {
  if (!linkInput.trim()) return;

  setFormData(prev => ({
    ...prev,
    links: [...prev.links, { url: linkInput, label: linkInput }],
  }));
  setLinkInput('');
};

const handleRecurrenceChange = (e) => {
  const { name, value, checked } = e.target;

  setFormData(prev => {
    const next = { ...prev.recurrence };

  if (name === 'freq') {
if (!value) {
  setExceptionDates([]);
  return {
    ...prev,
    recurrence: { freq: '', intervalCount: 1, byDay: [], byMonthDay: '', until: '' },
  };
}


  if (value === 'DAILY') {
    return {
      ...prev,
      recurrence: { ...prev.recurrence, freq: 'DAILY', byDay: [], byMonthDay: '' },
    };
  }

  if (value === 'WEEKLY') {
    return {
      ...prev,
      recurrence: { ...prev.recurrence, freq: 'WEEKLY', byMonthDay: '', byDay: [] }, // ✅ 월 규칙/혼합 제거
    };
  }

  if (value === 'MONTHLY') {
    return {
      ...prev,
      recurrence: { ...prev.recurrence, freq: 'MONTHLY', byDay: [], byMonthDay: '' },
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

const addMonthlyByDayRule = () => {
  const ord = monthlyOrdinal.trim();
  if (ord === '') return alert('(+/-숫자)를 입력하세요. 예: +2, -1');

  const n = Number(ord);
  // 요구사항 예: +2, -1 / 일반적으로 -5~ -1 또는 1~5
  if (!Number.isInteger(n) || n === 0 || n < -5 || n > 5) {
    alert('숫자는 -5 ~ -1 또는 1 ~ 5 형태로 입력하세요. (0 불가)');
    return;
  }

  const rule = `${n}${monthlyWeekday}`; // 예: "-2WE", "1MO"
  setFormData(prev => {
    const prevByDay = Array.isArray(prev.recurrence.byDay) ? prev.recurrence.byDay : [];
    // 중복 방지
    const nextByDay = prevByDay.includes(rule) ? prevByDay : [...prevByDay, rule];

    return {
      ...prev,
      recurrence: {
        ...prev.recurrence,
        byDay: nextByDay,
        byMonthDay: '', // BYDAY 모드면 날짜 규칙 비움
      },
    };
  });

  setMonthlyOrdinal('');
  setMonthlyWeekday('MO');
};
const handleSearchServiceUser = async (q) => {
  const query = String(q || '').trim();
  if (!query) {
    setServiceUserResults([]);
    return;
  }
  try {
    setSearchingServiceUser(true);
    const res = await ApiService.searchMembers(query, 0, 20);
    const payload = res?.data ?? res;        
    const page = payload?.data ?? payload;  
    const list = page?.content ?? [];       

    setServiceUserResults(Array.isArray(list) ? list : []);
  } catch (e) {
    console.error('[searchMembers] failed', e);
    setServiceUserResults([]);
    alert('회원 검색 실패');
  } finally {
    setSearchingServiceUser(false);
  }
};

const applyMonthlyByMonthDay = () => {
  const monthDay = monthlyMonthDay.trim();
  if (monthDay === '') return alert('날짜(1~31)를 입력하세요.');

  const d = Number(monthDay);
  if (!Number.isInteger(d) || d < 1 || d > 31) {
    alert('ByMonthDay는 1~31 날짜로 입력하세요.');
    return;
  }

  setFormData(prev => ({
    ...prev,
    recurrence: {
      ...prev.recurrence,
      byDay: [],     // BYMONTHDAY 모드면 요일 규칙 비움
      byMonthDay: d,
    },
  }));
};

const removeMonthlyByDayRule = (rule) => {
  setFormData(prev => ({
    ...prev,
    recurrence: {
      ...prev.recurrence,
      byDay: (prev.recurrence.byDay || []).filter(r => r !== rule),
    },
  }));
};

const openMonthlyRuleModal = () => {
  const rec = formData.recurrence;
  if (Array.isArray(rec.byDay) && rec.byDay.length > 0) {
    setMonthlyMode('BYDAY');
    setMonthlyOrdinal('');
    setMonthlyWeekday('MO');
    setMonthlyMonthDay('');
  } else {
    setMonthlyMode('BYMONTHDAY');
    setMonthlyOrdinal('');
    setMonthlyWeekday('MO');
    setMonthlyMonthDay(rec.byMonthDay ? String(rec.byMonthDay) : '');
  }
  setIsMonthlyRuleOpen(true);
};

const handleSubmit = async (e) => {
  e.preventDefault();

  let recurrencePayload = null;
  let scheduleRes = null;
  let newId = null;

  try {
    recurrencePayload = buildRecurrencePayload(formData.recurrence);
      console.log('🧪 formData.recurrence =', formData.recurrence);
      console.log('🧪 recurrencePayload =', recurrencePayload);
  } catch (err) {
    alert(err.message);
    return;
  }

  try {
    const payload = {
      title: formData.title ?? '',
      memo: formData.description ?? '',
      startAt: localInputToISO(formData.startDateTime),
      endAt: localInputToISO(formData.endDateTime),
      color: formData.color || '#3b82f6',
      places: [],
      links: formData.links,
      reminders: Array.isArray(formData.reminders)
        ? formData.reminders
            .map(r => (typeof r === 'object' && Number.isFinite(r.minutesBefore) ? { minutesBefore: r.minutesBefore } : null))
            .filter(Boolean)
        : [],
      todos: (formData.todos || []).map((t, i) => ({
        content: t.content ?? '',
        isDone: !!t.isDone,
        orderNo: i + 1,
      })),
    };

    // ✅ 일정 생성(여기서만 newId를 “처음” 만든다)
    scheduleRes = await createSchedule(payload);
    newId = extractScheduleId(scheduleRes);

    console.log('✅ [CREATE] res=', scheduleRes);
    console.log('✅ [CREATE] newId=', newId);

    if (!newId) throw new Error('일정 생성 응답에 id가 없습니다.');

// ✅ 반복 생성 (스케줄 생성 API와 별개)
let recurrenceId = null;

if (recurrencePayload) {
  // Swagger 스펙 키/타입 준수
  const recurrenceData = {
    ...recurrencePayload,
    // Swagger에 exceptions가 있으니 같이 보내는 것이 가장 단순
    exceptions: Array.isArray(exceptionDates) ? exceptionDates : [],
  };

  console.log('➡️ [RECURRENCE POST] scheduleId=', newId, recurrenceData);
  const recRes = await ApiService.createRecurrence(newId, recurrenceData);
  console.log('⬅️ [RECURRENCE POST] res=', recRes);

  // 응답 구조가 { data: { recurrenceId } } 또는 axios 형태일 수 있으니 방어적으로 파싱
  recurrenceId =
    recRes?.data?.recurrenceId ??
    recRes?.data?.data?.recurrenceId ??
    null;
}


if (Array.isArray(invitees) && invitees.length > 0) {
  const toBody = (inv) => {
    if (inv.type === 'SERVICE_USER') {
      const body = { mode: 'SERVICE_USER', memberId: inv.memberId };
      if (inv.nameOverride && String(inv.nameOverride).trim()) {
        body.name = String(inv.nameOverride).trim();
      }
      return body;
    }
    // NAME
    return { mode: 'NAME', name: String(inv.name).trim() };
  };

  const results = await Promise.allSettled(
    invitees.map((inv) => ApiService.addScheduleParticipant(newId, toBody(inv)))
  );

  const failCount = results.filter(r => r.status === 'rejected').length;
  if (failCount > 0) {
    alert(`참가자 초대 ${failCount}건 실패했습니다. (일정은 생성됨)`);
  }
}

// 2️⃣ 투두 저장 (Swagger: POST /api/schedules/{id}/todos { content })
if (Array.isArray(formData.todos) && formData.todos.length > 0) {
  for (const t of formData.todos) {
    const content = (t?.content ?? '').trim();
    if (!content) continue;

    console.log('➡️ [TODO POST] scheduleId=', newId, 'content=', content);
    const todoRes = await ApiService.addTodo(newId, content);
    console.log('⬅️ [TODO POST] res=', todoRes);
  }
}

// 3️⃣ 리마인더 저장 (Swagger: POST /api/schedules/{id}/reminders { minutesBefore })
if (Array.isArray(formData.reminders) && formData.reminders.length > 0) {
  for (const r of formData.reminders) {
    const minutes = Number(r?.minutesBefore);
    if (!Number.isFinite(minutes)) continue;

    console.log('➡️ [REMINDER POST] scheduleId=', newId, 'minutesBefore=', minutes);
    const remRes = await ApiService.createReminder(newId, minutes);
    console.log('⬅️ [REMINDER POST] res=', remRes);
  }
}

// 4️⃣ 링크 저장 (Swagger: POST /api/schedules/{id}/links { url, label })
if (Array.isArray(formData.links) && formData.links.length > 0) {
  for (const l of formData.links) {
    const url = (l?.url ?? '').trim();
    if (!url) continue;
    const label = (l?.label ?? url).trim();

    console.log('➡️ [LINK POST] scheduleId=', newId, { url, label });
    const linkRes = await ApiService.createScheduleLink(newId, { url, label });
    console.log('⬅️ [LINK POST] res=', linkRes);
  }
}

// 2️⃣ 지출 생성 (세부 항목 있을 때만)
if (expenseName && expenseLines.length > 0) {
const totalAmount = expenseLines.reduce(
  (sum, l) => sum + Number(l.lineAmount || 0),
  0
);

  const expenseRes = await ApiService.createExpense(newId, {
    name: expenseName,
    amount: totalAmount,
    paidAt: new Date().toISOString(),
  });

  const expenseId = expenseRes?.data?.expenseId;

for (const line of expenseLines) {
  const label = (line.label ?? '').trim();
  const quantity = Number(line.quantity);
  const unitAmount = Number(line.unitAmount);
  const lineAmount = Number(line.lineAmount);

  if (!label) continue;
  if (!Number.isInteger(quantity) || quantity <= 0) continue;
  if (!Number.isFinite(unitAmount) || unitAmount < 0) continue;

  // lineAmount는 프론트에서 계산했지만 안전하게 한 번 더 맞춤
  const safeLineAmount = quantity * unitAmount;

  await ApiService.createExpenseLine(expenseId, {
    label,
    quantity,
    unitAmount,
    lineAmount: safeLineAmount,
  });
} }

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
 const completeRes = await ApiService.completeUpload(uploadId, objectKey, true);
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
        recurrence: { freq: '', intervalCount: 1, byDay: [], byMonthDay: '', until: '' },
        todos: [],
        reminders: [],
      }));
      setNewTodo('');
    }
  }, [isOpen, selectedDate]);

useEffect(() => {
  if (!isOpen) return;

  setServiceUserResults([]);
  setInvitees([]);

  if (!currentCalendar?.id) return;

  (async () => {
    try {
      const [mRes, fRes] = await Promise.all([
        // ✅ 캘린더 멤버: status=ACCEPTED로 요청 (ApiService가 opts 지원해야 함)
        ApiService.getCalendarMembers(currentCalendar.id, {
          status: 'ACCEPTED',
          sort: 'NAME_ASC',
          page: 0,
          size: 200,
        }),
        // ✅ 친구: 이미 ACCEPTED만 오므로 페이징만 명시적으로
        ApiService.getFriends(0, 200),
      ]);

      const rawMembers = mRes?.data?.data?.content || mRes?.data?.content || [];
      const rawFriends = fRes?.data?.data?.content || fRes?.data?.content || [];

      // ✅ 내 id 추론 (응답에 myId가 같이 내려온다고 했으니 여기서 뽑아둠)
      const inferredMyId =
        (Array.isArray(rawMembers) && rawMembers.find((x) => x?.myId != null)?.myId) ?? null;
      setMyMemberId(inferredMyId);

      // ✅ 캘린더 멤버: ACCEPTED만 + 이름은 memberName → name으로 + 내 자신 제외
      const normalizedCalendarMembers = (Array.isArray(rawMembers) ? rawMembers : [])
        .filter((m) => m?.status === 'ACCEPTED') // 서버에서 이미 필터해도 안전망
        .filter((m) =>
          inferredMyId == null ? true : String(m?.memberId) !== String(inferredMyId)
        )
        .map((m) => ({
          memberId: m.memberId,
          name: m.memberName,   // ✅ 화면 표시용
          // email/phoneNumber가 있으면 같이 붙여도 됨(없으면 생략 가능)
          email: m.email,
          phoneNumber: m.phoneNumber,
        }));

      // ✅ 친구: friendName → name으로 + (원하면) 내 자신 제외 방어
      const normalizedFriends = (Array.isArray(rawFriends) ? rawFriends : [])
        .filter((f) =>
          inferredMyId == null ? true : String(f?.memberId) !== String(inferredMyId)
        )
        .map((f) => ({
          memberId: f.memberId,
          name: f.friendName,   // ✅ 화면 표시용
        }));

      setCalendarMembers(normalizedCalendarMembers);
      setFriends(normalizedFriends);
    } catch (e) {
      setCalendarMembers([]);
      setFriends([]);
      setMyMemberId(null);
    }
  })();
}, [isOpen, currentCalendar?.id]);


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
    setFormData(prev => ({
      ...prev,
      reminders: [{ minutesBefore: minutes }]
    }));
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
    setIsSearchingPlace(true);
    const res = await ApiService.searchPlaces(query);
    const list = res?.data?.content || [];

    if (!Array.isArray(list) || list.length === 0) {
      alert('검색 결과가 없습니다.');
      return;
    }

    // 🔥 검색 결과를 state에 저장
    setPlaceSearchResults(list);
  } catch (err) {
    alert('장소 검색 중 오류가 발생했습니다.');
  } finally {
    setIsSearchingPlace(false);
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

  const calcLineAmount = (q, u) => {
  const quantity = Number(q);
  const unitAmount = Number(u);
  if (!Number.isFinite(quantity) || quantity <= 0) return 0;
  if (!Number.isFinite(unitAmount) || unitAmount < 0) return 0;
  return quantity * unitAmount;
};

const handleAddExpenseLine = () => {
  setExpenseLines(prev => [
    ...prev,
    { label: '', quantity: 1, unitAmount: 0, lineAmount: 0 },
  ]);
};

const handleChangeExpenseLine = (idx, key, value) => {
  setExpenseLines(prev => {
    const next = [...prev];
    const cur = { ...next[idx] };

    if (key === 'label') cur.label = value;
    if (key === 'quantity') cur.quantity = value === '' ? '' : Number(value);
    if (key === 'unitAmount') cur.unitAmount = value === '' ? '' : Number(value);

    // ✅ lineAmount는 자동 계산 (quantity * unitAmount)
    const q = cur.quantity === '' ? 0 : cur.quantity;
    const u = cur.unitAmount === '' ? 0 : cur.unitAmount;
    cur.lineAmount = calcLineAmount(q, u);

    next[idx] = cur;
    return next;
  });
};

const handleRemoveExpenseLine = (idx) => {
  setExpenseLines(prev => prev.filter((_, i) => i !== idx));
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

const extractScheduleId = (res) => {
  if (!res) return undefined;
  if (res?.scheduleId) return res.scheduleId;
  if (res?.id) return res.id;
  if (res?.data?.scheduleId) return res.data.scheduleId;
  if (res?.data?.id) return res.data.id;
  if (res?.data?.data?.scheduleId) return res.data.data.scheduleId; // axios 형태 대비
  if (res?.data?.data?.id) return res.data.data.id;
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

            {/* 📍 장소 (생성 중 로컬로만 관리) */}
<div style={sectionStyle}>
  <label style={labelStyle}>📍 장소</label>

  {/* 검색 버튼 */}
  <div style={{ display: 'flex', gap: '0.5rem' }}>
    <button
      type="button"
      onClick={handleAddPlace}
      style={subButton}
      title="장소 검색"
    >
      <Search size={16} />
    </button>

    {/* ✅ 이미 추가된 장소 리스트 */}
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
            <span
              style={{
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {p.name || p.title}
            </span>

            <div style={{ display: 'flex', gap: '0.25rem' }}>
              <button
                type="button"
                onClick={() => handleReorderPlaces('up', i)}
                style={iconButton}
              >
                ↑
              </button>
              <button
                type="button"
                onClick={() => handleReorderPlaces('down', i)}
                style={iconButton}
              >
                ↓
              </button>
              <button
                type="button"
                onClick={() => handleRemovePlace(i)}
                style={iconButton}
              >
                <Trash2 size={14} />
              </button>
            </div>
          </div>
        ))
      ) : (
        <p style={{ color: '#9ca3af', fontSize: '0.875rem' }}>
          장소 없음
        </p>
      )}
    </div>
  </div>

  {/* 🔍 검색 결과 리스트 (👇 여기만 새로 추가) */}
  {placeSearchResults.length > 0 && (
    <div style={{ marginTop: 8 }}>
      <p style={{ fontSize: '0.8rem', color: '#6b7280' }}>
        장소를 클릭해서 추가하세요
      </p>

      {placeSearchResults.map((p) => (
        <div
          key={p.providerPlaceKey}
          onClick={() => {
            const newPlace = {
              mode: 'PROVIDER',
              provider: p.provider || 'NAVER',
              providerPlaceKey: p.providerPlaceKey,
              title: p.title,
              category: p.category || '',
              address: p.address || '',
              roadAddress: p.roadAddress || '',
              lat: Number(p.lat),
              lng: Number(p.lng),
              name: p.title,
            };

            setFormData(prev => ({
              ...prev,
              places: [...prev.places, newPlace],
            }));

            // ✅ 선택 후 검색 결과 닫기
            setPlaceSearchResults([]);
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
</div>
<ScheduleParticipantsSection
  invitees={invitees}
  setInvitees={setInvitees}
  calendarMembers={calendarMembers}
  friends={friends}
  serviceUserResults={serviceUserResults}
  searchingServiceUser={searchingServiceUser}
  onSearchServiceUser={handleSearchServiceUser}
  myMemberId={myMemberId}  />
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
    value={formData.recurrence.freq || ''}
    onChange={handleRecurrenceChange}
    style={inputStyle}
  >
   <option value="">없음</option>
<option value="DAILY">DAILY</option>
<option value="WEEKLY">WEEKLY</option>
<option value="MONTHLY">MONTHLY</option>
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

  {/* 반복 규칙 */}
  <label style={labelStyle}>반복 규칙</label>

  {/* ✅ MONTHLY: 상세 설정 모달로만 입력 */}
  {formData.recurrence.freq === 'MONTHLY' && (
    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      <button type="button" onClick={openMonthlyRuleModal} style={subButton}>
        매월 상세 설정
      </button>
<span style={{ fontSize: '0.85rem', color: '#6b7280' }}>
  {Array.isArray(formData.recurrence.byDay) && formData.recurrence.byDay.length > 0
    ? `ByDay: ${formData.recurrence.byDay.join(', ')}`
    : formData.recurrence.byMonthDay
      ? `ByMonthDay: ${formData.recurrence.byMonthDay}`
      : '설정 없음'}
</span>

      <button
        type="button"
        onClick={() => {
          // ✅ 월 규칙 초기화
          setFormData(prev => ({
            ...prev,
            recurrence: { ...prev.recurrence, byDay: [], byMonthDay: '' },
          }));
        }}
        style={subButton}
        title="월 반복 규칙 초기화"
      >
        초기화
      </button>
    </div>
  )}

  {/* ✅ WEEKLY: 요일 체크박스 */}
  {formData.recurrence.freq === 'WEEKLY' && (
    <div style={{ display: 'flex', gap: '0.5rem' }}>
      {['MO', 'TU', 'WE', 'TH', 'FR', 'SA', 'SU'].map((day) => (
        <label key={day} style={{ display: 'flex', alignItems: 'center' }}>
          <input
            type="checkbox"
            name="byDay"
            value={day}
            checked={Array.isArray(formData.recurrence.byDay) ? formData.recurrence.byDay.includes(day) : false}
            onChange={handleRecurrenceChange}
            style={{ marginRight: '0.5rem' }}
          />
          {day}
        </label>
      ))}
    </div>
  )}

  {/* ✅ DAILY/없음: 요일 규칙 표시 안 함 (필요하면 안내문만) */}
  {(formData.recurrence.freq === 'DAILY' || !formData.recurrence.freq) && (
    <div style={{ fontSize: '0.8rem', color: '#6b7280' }}>
      {formData.recurrence.freq === 'DAILY' ? '매일 반복은 요일 설정이 필요 없습니다.' : ''}
    </div>
  )}

  {/* 반복 종료일 */}
  <label style={labelStyle}>반복 종료일</label>
  <input
    type="datetime-local"
    name="until"
    value={formData.recurrence.until || ''}
    onChange={handleRecurrenceChange}
    style={inputStyle}
  />

{/* 반복 예외 날짜 선택 (✅ 반복이 있을 때는 언제나 가능) */}
{formData.recurrence.freq && (
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
        setExceptionDates(prev => (prev.includes(d) ? prev : [...prev, d]));
      }}
      style={inputStyle}
    />

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
              marginBottom: 4,
            }}
          >
            <span>{d}</span>
            <button
              type="button"
              onClick={() => setExceptionDates(prev => prev.filter(x => x !== d))}
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

  {/* 🔥 리마인더 추가 */}
  <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
   <select
  value={
    formData.reminders?.[0]?.minutesBefore
      ? minutesToSelect(formData.reminders[0].minutesBefore)
      : 'none'
  }
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
</div>

          {/* URL 링크 추가 (텍스트 입력 필드로 수정) */}
        <div style={sectionStyle}>
          <label style={labelStyle}>🌐 링크</label>
          <div>
          <input
  type="text"
  placeholder="URL을 입력하세요"
  value={linkInput}
  onChange={(e) => setLinkInput(e.target.value)}
  style={inputStyle}
/>
<button type="button" onClick={handleAddLink} style={subButton}>
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

  <button type="button" onClick={handleAddExpenseLine} style={subButton}>
    + 세부 지출 추가
  </button>

  {expenseLines.map((line, idx) => (
    <div
      key={idx}
      style={{
        background: '#f9fafb',
        border: '1px solid #e5e7eb',
        borderRadius: 8,
        padding: 10,
        marginTop: 8,
      }}
    >
      <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
        <input
          type="text"
          placeholder="항목명 (label)"
          value={line.label}
          onChange={(e) => handleChangeExpenseLine(idx, 'label', e.target.value)}
          style={{ ...inputStyle, marginBottom: 0, flex: 1 }}
        />

        <button
          type="button"
          onClick={() => handleRemoveExpenseLine(idx)}
          style={{ ...subButton, color: '#ef4444' }}
          title="세부 항목 삭제"
        >
          삭제
        </button>
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <input
          type="number"
          min="1"
          placeholder="수량 (quantity)"
          value={line.quantity}
          onChange={(e) => handleChangeExpenseLine(idx, 'quantity', e.target.value)}
          style={{ ...inputStyle, marginBottom: 0, width: 120 }}
        />
        <input
          type="number"
          min="0"
          placeholder="개당 금액 (unitAmount)"
          value={line.unitAmount}
          onChange={(e) => handleChangeExpenseLine(idx, 'unitAmount', e.target.value)}
          style={{ ...inputStyle, marginBottom: 0, width: 180 }}
        />

        <div style={{ flex: 1, textAlign: 'right', fontWeight: 600, alignSelf: 'center' }}>
          합계: {Number(line.lineAmount || 0).toLocaleString()}원
        </div>
      </div>
    </div>
  ))}

  {/* ✅ 전체 지출 합계(라인 합) */}
  {expenseLines.length > 0 && (
    <div style={{ marginTop: 10, textAlign: 'right', fontWeight: 700 }}>
      총 지출: {expenseLines.reduce((sum, l) => sum + Number(l.lineAmount || 0), 0).toLocaleString()}원
    </div>
  )}

  {/* 📷 영수증 첨부 */}
  <label style={{ ...labelStyle, marginTop: 10 }}>📷 영수증 첨부</label>
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
        {/* ✅ MONTHLY RULE MODAL */}{isMonthlyRuleOpen && (
  <div style={{ ...overlayStyle, zIndex: 1100 }}>
    <div style={{ ...modalStyle, width: 460 }}>
      <h3 style={{ marginBottom: 12 }}>MONTHLY 반복 상세 설정</h3>

      {/* ===== 요일 설정 ===== */}
      <div style={{ marginBottom: 12, padding: 10, border: '1px solid #e5e7eb', borderRadius: 10 }}>
        <label style={{ ...labelStyle, display: 'flex', alignItems: 'center', gap: 8 }}>
          <input
            type="checkbox"
            checked={monthlyMode === 'BYDAY'}
            onChange={() => setMonthlyMode('BYDAY')}
          />
          요일 설정 (예: -2수 / 뒤에서 둘째 주 수요일)
        </label>

        <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 8 }}>
          예시: -2WE, +1MO (여러 개 추가 가능)
        </div>

        <div style={{ display: 'flex', gap: 8, alignItems: 'center', opacity: monthlyMode === 'BYDAY' ? 1 : 0.45 }}>
          <input
            type="text"
            value={monthlyOrdinal}
            onChange={(e) => setMonthlyOrdinal(e.target.value)}
            placeholder="예: +2 (둘째), -1 (마지막)"
            style={{ ...inputStyle, marginBottom: 0, flex: 1 }}
            disabled={monthlyMode !== 'BYDAY'}
          />

          <select
            value={monthlyWeekday}
            onChange={(e) => setMonthlyWeekday(e.target.value)}
            style={{ ...inputStyle, marginBottom: 0, width: 120 }}
            disabled={monthlyMode !== 'BYDAY'}
          >
            <option value="MO">월</option>
            <option value="TU">화</option>
            <option value="WE">수</option>
            <option value="TH">목</option>
            <option value="FR">금</option>
            <option value="SA">토</option>
            <option value="SU">일</option>
          </select>

          <button
            type="button"
            onClick={addMonthlyByDayRule}
            style={saveButton}
            disabled={monthlyMode !== 'BYDAY'}
          >
            추가
          </button>
        </div>

        {/* 현재 BYDAY 규칙 목록 */}
        {Array.isArray(formData.recurrence.byDay) && formData.recurrence.byDay.length > 0 && (
          <div style={{ marginTop: 10 }}>
            <div style={{ fontSize: 12, color: '#374151', marginBottom: 6 }}>
              현재 요일 규칙:
            </div>

            {formData.recurrence.byDay.map((rule) => (
              <div
                key={rule}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  background: '#f9fafb',
                  border: '1px solid #e5e7eb',
                  padding: '6px 10px',
                  borderRadius: 8,
                  marginBottom: 6,
                }}
              >
                <span>{rule}</span>
                <button
                  type="button"
                  onClick={() => removeMonthlyByDayRule(rule)}
                  style={{ ...iconButton, color: '#ef4444' }}
                >
                  삭제
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ===== 날짜 설정 ===== */}
      <div style={{ marginBottom: 12, padding: 10, border: '1px solid #e5e7eb', borderRadius: 10 }}>
        <label style={{ ...labelStyle, display: 'flex', alignItems: 'center', gap: 8 }}>
          <input
            type="checkbox"
            checked={monthlyMode === 'BYMONTHDAY'}
            onChange={() => setMonthlyMode('BYMONTHDAY')}
          />
          날짜 설정 (예: 20 / 20일에 반복)
        </label>

        <div style={{ display: 'flex', gap: 8, alignItems: 'center', opacity: monthlyMode === 'BYMONTHDAY' ? 1 : 0.45 }}>
          <input
            type="number"
            value={monthlyMonthDay}
            onChange={(e) => setMonthlyMonthDay(e.target.value)}
            placeholder="1~31"
            style={{ ...inputStyle, marginBottom: 0, flex: 1 }}
            disabled={monthlyMode !== 'BYMONTHDAY'}
          />

          <button
            type="button"
            onClick={applyMonthlyByMonthDay}
            style={saveButton}
            disabled={monthlyMode !== 'BYMONTHDAY'}
          >
            적용
          </button>
        </div>

        <div style={{ fontSize: 12, color: '#6b7280', marginTop: 8 }}>
          현재 날짜 규칙: {formData.recurrence.byMonthDay ? String(formData.recurrence.byMonthDay) : '없음'}
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
        <button type="button" onClick={() => setIsMonthlyRuleOpen(false)} style={cancelButton}>
          닫기
        </button>
        <button
          type="button"
          onClick={() => {
            // 모드에 맞춰서 서로의 값 정리(선택적으로)
            if (monthlyMode === 'BYDAY') {
              setFormData(prev => ({
                ...prev,
                recurrence: { ...prev.recurrence, byMonthDay: '' },
              }));
            } else {
              setFormData(prev => ({
                ...prev,
                recurrence: { ...prev.recurrence, byDay: [] },
              }));
            }
            setIsMonthlyRuleOpen(false);
          }}
          style={saveButton}
        >
          완료
        </button>
      </div>
    </div>
  </div>
)}

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
