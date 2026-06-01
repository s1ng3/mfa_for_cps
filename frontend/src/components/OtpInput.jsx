import { useRef } from 'react';

// Six-box numeric OTP entry. Calls onChange with the concatenated string.
export default function OtpInput({ value, onChange, length = 6 }) {
  const refs = useRef([]);
  const chars = value.padEnd(length).split('').slice(0, length);

  const setChar = (i, c) => {
    const next = chars.map((ch, idx) => (idx === i ? c : ch)).join('').replace(/\s/g, '');
    onChange(next);
  };

  const handle = (i, e) => {
    const c = e.target.value.replace(/\D/g, '').slice(-1);
    setChar(i, c || ' ');
    if (c && i < length - 1) refs.current[i + 1]?.focus();
  };

  const handleKey = (i, e) => {
    if (e.key === 'Backspace' && !chars[i].trim() && i > 0) refs.current[i - 1]?.focus();
  };

  const handlePaste = (e) => {
    const digits = (e.clipboardData.getData('text') || '').replace(/\D/g, '').slice(0, length);
    if (digits) { e.preventDefault(); onChange(digits); }
  };

  return (
    <div className="row" style={{ gap: 8, justifyContent: 'space-between' }} onPaste={handlePaste}>
      {Array.from({ length }).map((_, i) => (
        <input
          key={i}
          ref={(el) => (refs.current[i] = el)}
          inputMode="numeric"
          maxLength={1}
          value={chars[i]?.trim() || ''}
          onChange={(e) => handle(i, e)}
          onKeyDown={(e) => handleKey(i, e)}
          style={{ textAlign: 'center', fontSize: 22, fontWeight: 700, padding: '12px 0' }}
        />
      ))}
    </div>
  );
}
