import React, { useState } from 'react';


 export default function AccordionSection({ title, count = 0, onOpenLoad, tone = 'indigo', children }) {
  const [open, setOpen] = useState(false);

  const handleToggle = async () => {
    const next = !open;
    setOpen(next);
    if (next && typeof onOpenLoad === 'function') {
      await onOpenLoad();
    }
  };

  const tones = {
   indigo: { chipBg: '#EEF2FF', chipFg: '#3730A3' },
   sky:    { chipBg: '#E0F2FE', chipFg: '#0369A1' },
   amber:  { chipBg: '#FEF3C7', chipFg: '#92400E' },
   emerald:{ chipBg: '#D1FAE5', chipFg: '#065F46' },
 };
 const palette = tones[tone] || tones.indigo;

  return (
    <div>
      <button onClick={handleToggle} style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
        <span>{title}</span>
        <span style={{ background: '#eef2ff', color: '#3730a3', borderRadius: 8, padding: '0 8px', fontSize: 12 }}>
          {count /* ✅ prop 직접 사용: 상태로 복제하지 않음 */}
        </span>
      </button>
      {open && <div style={{ marginTop: 8 }}>{children}</div>}
    </div>
  );
}
