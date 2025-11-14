// src/components/ExpenseModal.js
import React, { useEffect, useMemo, useState } from 'react';
import { ApiService } from '../services/apiService';

const overlayStyle = {
  position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
  backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1100,
};
const modalStyle = {
  backgroundColor: '#fff', padding: '1.25rem', borderRadius: '12px', width: 560, maxWidth: '92%', maxHeight: '90vh', overflowY: 'auto',
};
const inputStyle = {
  width: '100%', marginBottom: '0.5rem', padding: '0.5rem', borderRadius: '0.5rem', border: '1px solid #d1d5db', fontSize: 14,
};
const sectionStyle = { marginBottom: '1rem' };
const labelStyle = { fontWeight: 600, fontSize: 13, marginBottom: 6, display: 'block' };
const row = { display: 'flex', alignItems: 'center', gap: 8 };
const btn = {
  padding: '0.5rem 0.75rem', borderRadius: 8, border: 'none', cursor: 'pointer',
  background: '#e5e7eb', color: '#111', fontWeight: 500,
};
const btnPrimary = { ...btn, background: '#2563eb', color: '#fff' };
const btnDanger = { ...btn, background: '#ef4444', color: '#fff' };
const listRow = {
  display: 'grid', gridTemplateColumns: '1fr 120px 180px 96px', gap: 8,
  alignItems: 'center', background: '#f9fafb', padding: '8px 10px', borderRadius: 10,
};

export default function ExpenseModal({ isOpen, onClose, scheduleId }) {
  const [loading, setLoading] = useState(false);
  const [pageData, setPageData] = useState({ count: 0, expenses: [] });

  // 등록 폼
  const [name, setName] = useState('');
  const [amount, setAmount] = useState(''); // string로 받아서 parseInt
  const [paidAt, setPaidAt] = useState(''); // datetime-local
  const [receiptFile, setReceiptFile] = useState(null); // 영수증(옵션)

  const totalAmount = useMemo(
    () => (pageData.expenses || []).reduce((sum, e) => sum + (Number(e.amount) || 0), 0),
    [pageData.expenses]
  );

  const resetForm = () => {
    setName('');
    setAmount('');
    setPaidAt('');
    setReceiptFile(null);
  };

  const loadExpenses = async () => {
    if (!scheduleId) return;
    setLoading(true);
    try {
      // GET /api/schedules/{scheduleId}/expenses
      const res = await ApiService.listExpenses(scheduleId);
      // 명세: data: { count, expenses: [] }
      const data = res?.data ?? res; // 래핑 대비
      setPageData({
        count: data?.data?.count ?? data?.count ?? 0,
        expenses: data?.data?.expenses ?? data?.expenses ?? [],
      });
    } catch (err) {
      console.error('[expenses:list] error', err);
      alert('지출 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isOpen) return;
    loadExpenses();
  }, [isOpen]); // eslint-disable-line
const handleCreate = async () => {
  try {
    // ✅ 1️⃣ 영수증 파일(OCR 자동 모드)
    if (receiptFile && (!name || !amount)) {
      console.log('[ExpenseModal] OCR 모드 - 영수증 업로드 시작:', receiptFile);

      const presign = await ApiService.getPresignedUrl(scheduleId, receiptFile, true);
      const { uploadId, objectKey, presignedUrl } = presign.data;

      await fetch(presignedUrl, {
        method: 'PUT',
        headers: { 'Content-Type': receiptFile.type },
        body: receiptFile,
        mode: 'cors',
        credentials: 'omit',
      });
      console.log('[ExpenseModal] S3 업로드 완료:', { uploadId, objectKey });

      const completeRes = await ApiService.completeUpload(uploadId, objectKey);
      console.log('[ExpenseModal] OCR 완료 및 DB 반영 결과:', completeRes);

      await loadExpenses();
      resetForm();
      alert('🧾 OCR 인식 요청이 완료되었습니다!');
      return;
    }

    // ✅ 2️⃣ 수동 입력 모드 (name, amount 모두 있을 때)
    if (name && amount) {
      const payload = {
        name,
        amount: parseInt(amount, 10),
        paidAt: paidAt ? new Date(paidAt).toISOString() : null,
      };
      const res = await ApiService.createExpense(scheduleId, payload);
      console.log('[ExpenseModal] 수동 지출 등록 완료:', res);

      // 선택적으로 영수증도 같이 업로드
      if (receiptFile) {
        const presign = await ApiService.getPresignedUrl(scheduleId, receiptFile, true);
        const { uploadId, objectKey, presignedUrl } = presign.data;

        await fetch(presignedUrl, {
          method: 'PUT',
          headers: { 'Content-Type': receiptFile.type },
          body: receiptFile,
          mode: 'cors',
          credentials: 'omit',
        });
        const completeRes = await ApiService.completeUpload(uploadId, objectKey);
        console.log('[ExpenseModal] 수동 + 영수증 OCR 완료:', completeRes);
      }

      await loadExpenses();
      resetForm();
      alert('✅ 지출이 등록되었습니다.');
      return;
    }

    // ✅ 3️⃣ 아무 입력도 없는 경우
    alert('지출명/금액을 입력하거나 영수증 파일을 첨부해주세요.');
  } catch (err) {
    console.error('[expenses:create] error', err);
    alert('지출 등록에 실패했습니다.');
  }
};


  const handleDelete = async (expenseId) => {
    if (!window.confirm('이 지출을 삭제할까요?')) return;
    try {
      await ApiService.deleteExpense(scheduleId, expenseId);
      await loadExpenses();
    } catch (err) {
      console.error('[expenses:delete] error', err);
      alert('삭제에 실패했습니다.');
    }
  };

  if (!isOpen) return null;

  return (
    <div style={overlayStyle}>
      <div style={modalStyle}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <h3 style={{ margin: 0, fontSize: 18 }}>💰 지출 관리</h3>
          <button onClick={onClose} style={btn}>닫기</button>
        </div>

        {/* 등록 폼 */}
        <div style={sectionStyle}>
          <label style={labelStyle}>새 지출 등록</label>
          <input
            type="text"
            placeholder="지출명 (예: 회식, 카페)"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={inputStyle}
          />
          <div style={row}>
            <input
              type="number"
              placeholder="금액"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              style={{ ...inputStyle, marginBottom: 0 }}
            />
            <input
              type="datetime-local"
              value={paidAt}
              onChange={(e) => setPaidAt(e.target.value)}
              style={{ ...inputStyle, marginBottom: 0 }}
            />
          </div>
          <div style={{ ...row, marginTop: 8 }}>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setReceiptFile(e.target.files?.[0] ?? null)}
              style={{ flex: 1 }}
            />
            <button onClick={handleCreate} style={btnPrimary}>등록</button>
          </div>
        </div>

        {/* 리스트 */}
        <div style={sectionStyle}>
          <label style={labelStyle}>지출 목록</label>

          <div style={{ ...listRow, background: 'transparent', padding: '4px 10px', fontWeight: 600 }}>
            <span>지출명</span>
            <span style={{ textAlign: 'right' }}>금액</span>
            <span>결제일시</span>
            <span style={{ textAlign: 'right' }}>작업</span>
          </div>

          {loading ? (
            <div style={{ padding: 12, color: '#6b7280' }}>불러오는 중…</div>
          ) : (pageData.expenses || []).length === 0 ? (
            <div style={{ padding: 12, color: '#9ca3af' }}>지출이 없습니다.</div>
          ) : (
            (pageData.expenses || []).map((e) => (
              <div key={e.expenseId} style={listRow}>
                <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {e.name}
                </div>
                <div style={{ textAlign: 'right' }}>
                  {Number(e.amount).toLocaleString()}원
                </div>
                <div>
                  {e.paidAt ? new Date(e.paidAt).toLocaleString() : '-'}
                </div>
                <div style={{ textAlign: 'right' }}>
                  {/* (선택) 상세 보기/수정은 추후 ExpenseDetailModal로 확장 */}
                  <button onClick={() => handleDelete(e.expenseId)} style={btnDanger}>삭제</button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* 합계 */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 6 }}>
          <div style={{ fontWeight: 700 }}>합계:</div>
          <div style={{ fontWeight: 700 }}>{totalAmount.toLocaleString()}원</div>
        </div>
      </div>
    </div>
  );
}
