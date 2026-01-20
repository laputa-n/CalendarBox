// src/components/ExpenseModal.js
import React, { useEffect, useMemo, useState, useCallback } from 'react';
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
const btnGhost = { ...btn, background: '#f3f4f6', color: '#111' };

const listRow = {
  display: 'grid', gridTemplateColumns: '1fr 120px 180px 160px', gap: 8,
  alignItems: 'center', background: '#f9fafb', padding: '8px 10px', borderRadius: 10,
};

// ✅ 라인(세부항목) 컬럼: 항목명 / 수량 / 개당금액 / 총금액 / 작업
const lineRow = {
  display: 'grid', gridTemplateColumns: '1fr 80px 120px 120px 140px', gap: 8,
  alignItems: 'center', background: '#f8fafc', padding: '8px 10px', borderRadius: 10,
};

export default function ExpenseModal({ isOpen, onClose, scheduleId }) {
  const unwrapData = useCallback((res) => {
    const body = res?.data ?? res;
    return body?.data ?? body;
  }, []);

  const calcLineAmount = useCallback((q, u) => {
    const quantity = parseInt(q, 10);
    const unitAmount = Number(u);
    if (!Number.isInteger(quantity) || quantity <= 0) return 0;
    if (!Number.isFinite(unitAmount) || unitAmount < 0) return 0;
    return quantity * unitAmount;
  }, []);

  const [loading, setLoading] = useState(false);
  const [pageData, setPageData] = useState({ count: 0, expenses: [] });

  // 등록 폼(기존)
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [paidAt, setPaidAt] = useState('');
  const [receiptFile, setReceiptFile] = useState(null);

  // ✅ 선택된 지출 + 라인
  const [selectedExpenseId, setSelectedExpenseId] = useState(null);
  const [loadingLines, setLoadingLines] = useState(false);
  const [lines, setLines] = useState([]);

  // ✅ 라인 추가(명세 반영)
  const [newLineLabel, setNewLineLabel] = useState('');
  const [newLineQuantity, setNewLineQuantity] = useState(1);
  const [newLineUnitAmount, setNewLineUnitAmount] = useState(0);

  // ✅ 라인 수정(명세 반영)
  const [editingLineId, setEditingLineId] = useState(null);
  const [editingLineLabel, setEditingLineLabel] = useState('');
  const [editingLineQuantity, setEditingLineQuantity] = useState(1);
  const [editingLineUnitAmount, setEditingLineUnitAmount] = useState(0);

  // ✅ 상위 expense 수정 상태
  const [editingExpenseId, setEditingExpenseId] = useState(null);
  const [editingExpenseName, setEditingExpenseName] = useState('');
  const [editingExpenseAmount, setEditingExpenseAmount] = useState('');
  const [editingExpensePaidAt, setEditingExpensePaidAt] = useState('');

  const totalAmount = useMemo(
    () => (pageData.expenses || []).reduce((sum, e) => sum + (Number(e.amount) || 0), 0),
    [pageData.expenses]
  );

  const selectedExpense = useMemo(() => {
    return (pageData.expenses || []).find(e => e.expenseId === selectedExpenseId) ?? null;
  }, [pageData.expenses, selectedExpenseId]);

  const linesTotal = useMemo(() => {
    return (lines || []).reduce((sum, l) => sum + (Number(l.lineAmount) || 0), 0);
  }, [lines]);

  const newLineComputedAmount = useMemo(() => {
    return calcLineAmount(newLineQuantity, newLineUnitAmount);
  }, [newLineQuantity, newLineUnitAmount, calcLineAmount]);

  const editingLineComputedAmount = useMemo(() => {
    return calcLineAmount(editingLineQuantity, editingLineUnitAmount);
  }, [editingLineQuantity, editingLineUnitAmount, calcLineAmount]);

  const resetForm = () => {
    setName('');
    setAmount('');
    setPaidAt('');
    setReceiptFile(null);
  };

  // ====== 조회 ======
  const loadExpenses = async () => {
    if (!scheduleId) return;
    setLoading(true);
    try {
      const res = await ApiService.listExpenses(scheduleId);
      const data = unwrapData(res);

      const next = {
        count: data?.count ?? 0,
        expenses: data?.expenses ?? [],
      };
      setPageData(next);

      // 선택 유지/정리
      if (selectedExpenseId) {
        const exists = (next.expenses || []).some(e => e.expenseId === selectedExpenseId);
        if (!exists) {
          setSelectedExpenseId(null);
          setLines([]);
        }
      }
    } catch (err) {
      console.error('[expenses:list] error', err);
      alert('지출 목록을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const loadLines = async (expenseId) => {
    if (!expenseId) return;
    setLoadingLines(true);
    try {
      const res = await ApiService.listExpenseLines(expenseId, 0, 200);
      const data = unwrapData(res);

      const list =
        Array.isArray(data?.content) ? data.content :
        Array.isArray(data?.lines) ? data.lines :
        Array.isArray(data) ? data :
        [];

      // ✅ 숫자 필드 정리(방어)
      const normalized = list.map((l) => ({
        ...l,
        expenseLineId: l.expenseLineId ?? l.id,
        label: l.label ?? '',
        quantity: Number.isFinite(Number(l.quantity)) ? parseInt(l.quantity, 10) : 1,
        unitAmount: Number.isFinite(Number(l.unitAmount)) ? Number(l.unitAmount) : 0,
        lineAmount: Number.isFinite(Number(l.lineAmount)) ? Number(l.lineAmount) : 0,
      }));

      setLines(normalized);
    } catch (err) {
      console.error('[lines:list] error', err);
      alert('세부 목록을 불러오지 못했습니다.');
      setLines([]);
    } finally {
      setLoadingLines(false);
    }
  };

  useEffect(() => {
    if (!isOpen) return;
    loadExpenses();
    setSelectedExpenseId(null);
    setLines([]);
    setEditingLineId(null);
    setEditingExpenseId(null);

    // 라인 입력 초기화
    setNewLineLabel('');
    setNewLineQuantity(1);
    setNewLineUnitAmount(0);
  }, [isOpen]); // eslint-disable-line

  // ====== 생성 ======
  const handleCreate = async (e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    try {
      // 1) OCR 자동 모드: 영수증만 첨부했고 name/amount 비었을 때
      if (receiptFile && (!name || !amount)) {
        const presign = await ApiService.getPresignedUrl(scheduleId, receiptFile, true);
        const { uploadId, objectKey, presignedUrl } = presign.data;

        await fetch(presignedUrl, {
          method: 'PUT',
          headers: { 'Content-Type': receiptFile.type },
          body: receiptFile,
          mode: 'cors',
          credentials: 'omit',
        });

        await ApiService.completeUpload(uploadId, objectKey, true);

        await loadExpenses();
        resetForm();
        alert('🧾 OCR 인식 요청이 완료되었습니다!');
        return;
      }

      // 2) 수동 입력 모드
      if (name && amount) {
        const payload = {
          name,
          amount: parseInt(amount, 10),
          paidAt: paidAt ? new Date(paidAt).toISOString() : null,
        };
        await ApiService.createExpense(scheduleId, payload);

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

          await ApiService.completeUpload(uploadId, objectKey, true);
        }

        await loadExpenses();
        resetForm();
        alert('✅ 지출이 등록되었습니다.');
        return;
      }

      alert('지출명/금액을 입력하거나 영수증 파일을 첨부해주세요.');
    } catch (err) {
      console.error('[expenses:create] error', err);
      alert('지출 등록에 실패했습니다.');
    }
  };

  // ====== 삭제 ======
  const handleDelete = async (expenseId, e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    if (!window.confirm('이 지출을 삭제할까요?')) return;
    try {
      await ApiService.deleteExpense(scheduleId, expenseId);
      await loadExpenses();
      if (selectedExpenseId === expenseId) {
        setSelectedExpenseId(null);
        setLines([]);
      }
      if (editingExpenseId === expenseId) {
        cancelEditExpense();
      }
    } catch (err) {
      console.error('[expenses:delete] error', err);
      alert('삭제에 실패했습니다.');
    }
  };

  // ====== 선택 ======
  const handleSelectExpense = async (expenseId) => {
    setSelectedExpenseId(expenseId);
    setEditingLineId(null);

    // 라인 입력 초기화
    setNewLineLabel('');
    setNewLineQuantity(1);
    setNewLineUnitAmount(0);

    await loadLines(expenseId);
  };

  // ====== 상위 expense 수정 ======
  const startEditExpense = (exp, e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    setEditingExpenseId(exp.expenseId);
    setEditingExpenseName(exp.name ?? '');
    setEditingExpenseAmount(String(exp.amount ?? ''));
    // paidAt은 datetime-local 값으로 보여줘야 해서 local 변환
    if (exp.paidAt) {
      const d = new Date(exp.paidAt);
      const pad = (n) => String(n).padStart(2, '0');
      const local = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
      setEditingExpensePaidAt(local);
    } else {
      setEditingExpensePaidAt('');
    }
  };

  const cancelEditExpense = (e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    setEditingExpenseId(null);
    setEditingExpenseName('');
    setEditingExpenseAmount('');
    setEditingExpensePaidAt('');
  };

  const handleUpdateExpense = async (expenseId, e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    if (!expenseId) return;
    if (!editingExpenseName.trim()) return alert('지출명을 입력하세요.');
    const amt = Number(editingExpenseAmount);
    if (!Number.isFinite(amt) || amt <= 0) return alert('금액을 올바르게 입력하세요.');

    try {
      await ApiService.updateExpense(scheduleId, expenseId, {
        name: editingExpenseName.trim(),
        amount: amt,
        paidAt: editingExpensePaidAt ? new Date(editingExpensePaidAt).toISOString() : null,
      });
      cancelEditExpense();
      await loadExpenses();
    } catch (err) {
      console.error('[expenses:update] error', err);
      alert('지출 수정 실패');
    }
  };

  // ===== 라인 CRUD (명세 반영) =====
  const handleAddLine = async (e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    if (!selectedExpenseId) return alert('먼저 지출을 선택하세요.');
    if (!newLineLabel.trim()) return alert('세부 항목명을 입력하세요.');

    const q = parseInt(newLineQuantity, 10);
    if (!Number.isInteger(q) || q <= 0) return alert('수량(quantity)을 1 이상 정수로 입력하세요.');

    const u = Number(newLineUnitAmount);
    if (!Number.isFinite(u) || u < 0) return alert('개당 금액(unitAmount)을 올바르게 입력하세요.');

    const lineAmount = q * u;

    try {
      await ApiService.createExpenseLine(selectedExpenseId, {
        label: newLineLabel.trim(),
        quantity: q,
        unitAmount: u,
        lineAmount,
      });

      setNewLineLabel('');
      setNewLineQuantity(1);
      setNewLineUnitAmount(0);

      await loadLines(selectedExpenseId);
    } catch (err) {
      console.error('[lines:create] error', err);
      alert('세부 항목 추가 실패');
    }
  };

  const startEditLine = (line, e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    const id = line.expenseLineId ?? line.id;
    setEditingLineId(id);
    setEditingLineLabel(line.label ?? '');
    setEditingLineQuantity(Number.isFinite(Number(line.quantity)) ? parseInt(line.quantity, 10) : 1);
    setEditingLineUnitAmount(Number.isFinite(Number(line.unitAmount)) ? Number(line.unitAmount) : 0);
  };

  const cancelEditLine = (e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    setEditingLineId(null);
    setEditingLineLabel('');
    setEditingLineQuantity(1);
    setEditingLineUnitAmount(0);
  };

  const handleUpdateLine = async (e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    if (!selectedExpenseId || !editingLineId) return;
    if (!editingLineLabel.trim()) return alert('항목명을 입력하세요.');

    const q = parseInt(editingLineQuantity, 10);
    if (!Number.isInteger(q) || q <= 0) return alert('수량(quantity)을 1 이상 정수로 입력하세요.');

    const u = Number(editingLineUnitAmount);
    if (!Number.isFinite(u) || u < 0) return alert('개당 금액(unitAmount)을 올바르게 입력하세요.');

    const lineAmount = q * u;

    try {
      await ApiService.updateExpenseLine(selectedExpenseId, editingLineId, {
        label: editingLineLabel.trim(),
        quantity: q,
        unitAmount: u,
        lineAmount,
      });
      cancelEditLine();
      await loadLines(selectedExpenseId);
    } catch (err) {
      console.error('[lines:update] error', err);
      alert('세부 항목 수정 실패');
    }
  };

  const handleDeleteLine = async (lineId, e) => {
    e?.preventDefault?.();
    e?.stopPropagation?.();

    if (!selectedExpenseId) return;
    if (!window.confirm('이 세부 항목을 삭제할까요?')) return;

    try {
      await ApiService.deleteExpenseLine(selectedExpenseId, lineId);
      await loadLines(selectedExpenseId);
    } catch (err) {
      console.error('[lines:delete] error', err);
      alert('세부 항목 삭제 실패');
    }
  };

  if (!isOpen) return null;

  return (
    <div style={overlayStyle}>
      <div style={modalStyle}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <h3 style={{ margin: 0, fontSize: 18 }}>💰 지출 관리</h3>
          <button type="button" onClick={onClose} style={btn}>닫기</button>
        </div>

        {/* 등록 폼(기존 그대로) */}
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
            <button type="button" onClick={handleCreate} style={btnPrimary}>등록</button>
          </div>
        </div>

        {/* 지출 목록 (선택 + 수정 가능) */}
        <div style={sectionStyle}>
          <label style={labelStyle}>지출 목록 (클릭하면 세부 목록 조회)</label>

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
            (pageData.expenses || []).map((e) => {
              const selected = e.expenseId === selectedExpenseId;
              const isEditingExpense = e.expenseId === editingExpenseId;

              return (
                <div
                  key={e.expenseId}
                  style={{
                    ...listRow,
                    cursor: 'pointer',
                    outline: selected ? '2px solid #93c5fd' : 'none',
                    background: selected ? '#eff6ff' : '#f9fafb',
                  }}
                  onClick={() => handleSelectExpense(e.expenseId)}
                  title="클릭하면 세부 항목이 아래에 표시됩니다"
                >
                  {/* 지출명 */}
                  <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {isEditingExpense ? (
                      <input
                        value={editingExpenseName}
                        onChange={(ev) => setEditingExpenseName(ev.target.value)}
                        style={{ ...inputStyle, marginBottom: 0 }}
                        onClick={(ev) => ev.stopPropagation()}
                      />
                    ) : (
                      e.name
                    )}
                  </div>

                  {/* 금액 */}
                  <div style={{ textAlign: 'right' }}>
                    {isEditingExpense ? (
                      <input
                        type="number"
                        value={editingExpenseAmount}
                        onChange={(ev) => setEditingExpenseAmount(ev.target.value)}
                        style={{ ...inputStyle, marginBottom: 0 }}
                        onClick={(ev) => ev.stopPropagation()}
                      />
                    ) : (
                      `${Number(e.amount).toLocaleString()}원`
                    )}
                  </div>

                  {/* 결제일시 */}
                  <div>
                    {isEditingExpense ? (
                      <input
                        type="datetime-local"
                        value={editingExpensePaidAt}
                        onChange={(ev) => setEditingExpensePaidAt(ev.target.value)}
                        style={{ ...inputStyle, marginBottom: 0 }}
                        onClick={(ev) => ev.stopPropagation()}
                      />
                    ) : (
                      e.paidAt ? new Date(e.paidAt).toLocaleString() : '-'
                    )}
                  </div>

                  {/* 작업 */}
                  <div style={{ textAlign: 'right', display: 'flex', justifyContent: 'flex-end', gap: 6 }}>
                    {isEditingExpense ? (
                      <>
                        <button
                          type="button"
                          onClick={(ev) => handleUpdateExpense(e.expenseId, ev)}
                          style={btnPrimary}
                        >
                          저장
                        </button>
                        <button
                          type="button"
                          onClick={cancelEditExpense}
                          style={btnGhost}
                        >
                          취소
                        </button>
                      </>
                    ) : (
                      <>
                        <button
                          type="button"
                          onClick={(ev) => startEditExpense(e, ev)}
                          style={btnGhost}
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          onClick={(ev) => handleDelete(e.expenseId, ev)}
                          style={btnDanger}
                        >
                          삭제
                        </button>
                      </>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* 합계 */}
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 6 }}>
          <div style={{ fontWeight: 700 }}>합계:</div>
          <div style={{ fontWeight: 700 }}>{totalAmount.toLocaleString()}원</div>
        </div>

        {/* 세부 목록(라인) */}
        <div style={{ marginTop: 16, paddingTop: 14, borderTop: '1px solid #e5e7eb' }}>
          <label style={labelStyle}>세부 목록 (라인)</label>

          {!selectedExpenseId ? (
            <div style={{ color: '#9ca3af', fontSize: 13, padding: 8 }}>
              지출 목록에서 항목을 클릭하면 세부 목록이 표시됩니다.
            </div>
          ) : (
            <>
              <div style={{ fontSize: 13, color: '#374151', marginBottom: 10 }}>
                <div><b>선택 지출:</b> {selectedExpense?.name ?? '-'}</div>
                <div><b>지출 금액:</b> {Number(selectedExpense?.amount ?? 0).toLocaleString()}원</div>
                <div><b>세부 합계:</b> {linesTotal.toLocaleString()}원</div>
              </div>

              {/* 라인 추가 */}
              <div style={{ ...sectionStyle, marginBottom: 10 }}>
                <div style={row}>
                  <input
                    type="text"
                    placeholder="항목명(label) (예: 아메리카노)"
                    value={newLineLabel}
                    onChange={(e) => setNewLineLabel(e.target.value)}
                    style={{ ...inputStyle, marginBottom: 0 }}
                  />
                  <input
                    type="number"
                    min="1"
                    placeholder="수량"
                    value={newLineQuantity}
                    onChange={(e) => setNewLineQuantity(e.target.value === '' ? '' : parseInt(e.target.value, 10))}
                    style={{ ...inputStyle, marginBottom: 0, width: 90 }}
                  />
                  <input
                    type="number"
                    min="0"
                    placeholder="개당금액"
                    value={newLineUnitAmount}
                    onChange={(e) => setNewLineUnitAmount(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ ...inputStyle, marginBottom: 0, width: 120 }}
                  />
                  <div style={{ width: 120, textAlign: 'right', fontWeight: 700 }}>
                    {newLineComputedAmount.toLocaleString()}원
                  </div>
                  <button type="button" onClick={handleAddLine} style={btnPrimary}>추가</button>
                </div>
              </div>

              {/* 라인 헤더 */}
              <div style={{ ...lineRow, background: 'transparent', padding: '4px 10px', fontWeight: 600 }}>
                <span>항목명</span>
                <span style={{ textAlign: 'right' }}>수량</span>
                <span style={{ textAlign: 'right' }}>개당</span>
                <span style={{ textAlign: 'right' }}>총액</span>
                <span style={{ textAlign: 'right' }}>작업</span>
              </div>

              {/* 라인 목록 */}
              {loadingLines ? (
                <div style={{ padding: 12, color: '#6b7280' }}>세부 목록 불러오는 중…</div>
              ) : (lines || []).length === 0 ? (
                <div style={{ padding: 12, color: '#9ca3af' }}>세부 항목이 없습니다.</div>
              ) : (
                (lines || []).map((l) => {
                  const lineId = l.expenseLineId ?? l.id;
                  const isEditing = lineId === editingLineId;

                  return (
                    <div key={lineId} style={lineRow}>
                      {/* label */}
                      <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {isEditing ? (
                          <input
                            value={editingLineLabel}
                            onChange={(e) => setEditingLineLabel(e.target.value)}
                            style={{ ...inputStyle, marginBottom: 0 }}
                          />
                        ) : (
                          l.label
                        )}
                      </div>

                      {/* quantity */}
                      <div style={{ textAlign: 'right' }}>
                        {isEditing ? (
                          <input
                            type="number"
                            min="1"
                            value={editingLineQuantity}
                            onChange={(e) => setEditingLineQuantity(e.target.value === '' ? '' : parseInt(e.target.value, 10))}
                            style={{ ...inputStyle, marginBottom: 0 }}
                          />
                        ) : (
                          Number(l.quantity ?? 0).toLocaleString()
                        )}
                      </div>

                      {/* unitAmount */}
                      <div style={{ textAlign: 'right' }}>
                        {isEditing ? (
                          <input
                            type="number"
                            min="0"
                            value={editingLineUnitAmount}
                            onChange={(e) => setEditingLineUnitAmount(e.target.value === '' ? '' : Number(e.target.value))}
                            style={{ ...inputStyle, marginBottom: 0 }}
                          />
                        ) : (
                          `${Number(l.unitAmount ?? 0).toLocaleString()}`
                        )}
                      </div>

                      {/* lineAmount */}
                      <div style={{ textAlign: 'right', fontWeight: 700 }}>
                        {isEditing
                          ? `${editingLineComputedAmount.toLocaleString()}원`
                          : `${Number(l.lineAmount ?? 0).toLocaleString()}원`}
                      </div>

                      {/* actions */}
                      <div style={{ textAlign: 'right', display: 'flex', justifyContent: 'flex-end', gap: 6 }}>
                        {isEditing ? (
                          <>
                            <button type="button" onClick={handleUpdateLine} style={btnPrimary}>저장</button>
                            <button type="button" onClick={cancelEditLine} style={btnGhost}>취소</button>
                          </>
                        ) : (
                          <>
                            <button type="button" onClick={(ev) => startEditLine(l, ev)} style={btnGhost}>수정</button>
                            <button type="button" onClick={(ev) => handleDeleteLine(lineId, ev)} style={btnDanger}>삭제</button>
                          </>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
