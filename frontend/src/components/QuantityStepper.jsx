import { useEffect, useRef, useState } from 'react';

export default function QuantityStepper({
  value,
  min = 1,
  max = 99,
  onChange,
  disabled = false,
  size = 'md', // 'sm' | 'md'
}) {
  const [draft, setDraft] = useState(String(value ?? min));
  const [editing, setEditing] = useState(false);
  const targetRef = useRef(value ?? min);
  const timerRef = useRef(null);

  // Sync external value, skip during user interaction so we don't clobber typing/holding.
  useEffect(() => {
    if (editing || timerRef.current != null) return;
    targetRef.current = value ?? min;
    setDraft(String(value ?? min));
  }, [value, min, editing]);

  useEffect(() => () => {
    if (timerRef.current) clearTimeout(timerRef.current);
  }, []);

  const clamp = (n) => {
    if (Number.isNaN(n)) return min;
    return Math.max(min, Math.min(max, n));
  };

  const commit = (n) => {
    const next = clamp(n);
    setDraft(String(next));
    targetRef.current = next;
    if (next !== value) onChange(next);
  };

  const startHold = (delta) => {
    if (disabled) return;
    if (timerRef.current) clearTimeout(timerRef.current);
    targetRef.current = clamp(targetRef.current + delta);
    setDraft(String(targetRef.current));

    let i = 0;
    const tick = () => {
      i++;
      const next = clamp(targetRef.current + delta);
      if (next === targetRef.current) {
        timerRef.current = null;
        return;
      }
      targetRef.current = next;
      setDraft(String(next));
      const delay = i < 5 ? 250 : i < 12 ? 100 : 60;
      timerRef.current = setTimeout(tick, delay);
    };
    timerRef.current = setTimeout(tick, 350);
  };

  const stopHold = () => {
    if (timerRef.current == null) return;
    clearTimeout(timerRef.current);
    timerRef.current = null;
    if (targetRef.current !== value) onChange(targetRef.current);
  };

  const handleType = (e) => {
    const raw = e.target.value;
    // Only digits — drop everything else
    const cleaned = raw.replace(/\D/g, '');
    setDraft(cleaned);
  };

  const commitDraft = () => {
    const n = parseInt(draft || String(min), 10);
    commit(n);
    setEditing(false);
  };

  const dims = size === 'sm'
    ? { btn: 'w-7 h-7 text-base', input: 'w-9 text-xs h-7' }
    : { btn: 'w-9 h-9 text-lg', input: 'w-12 text-sm h-9' };

  const buttonClass = `${dims.btn} font-bold flex items-center justify-center hover:bg-black hover:text-white transition-colors disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-black`;

  const numericDraft = parseInt(draft || '0', 10);
  const decDisabled = disabled || numericDraft <= min;
  const incDisabled = disabled || numericDraft >= max;

  return (
    <div className="inline-flex items-center border border-black/15 select-none bg-white">
      <button
        type="button"
        onMouseDown={() => startHold(-1)}
        onMouseUp={stopHold}
        onMouseLeave={stopHold}
        onTouchStart={(e) => { e.preventDefault(); startHold(-1); }}
        onTouchEnd={(e) => { e.preventDefault(); stopHold(); }}
        disabled={decDisabled}
        className={buttonClass}
        aria-label="Decrease quantity"
      >−</button>
      <input
        type="text"
        inputMode="numeric"
        value={draft}
        onChange={handleType}
        onFocus={(e) => { setEditing(true); e.target.select(); }}
        onBlur={commitDraft}
        onKeyDown={(e) => {
          if (e.key === 'Enter') { e.currentTarget.blur(); }
          if (e.key === 'Escape') { setDraft(String(value ?? min)); setEditing(false); e.currentTarget.blur(); }
        }}
        disabled={disabled}
        className={`${dims.input} text-center font-bold tabular-nums bg-transparent focus:outline-none focus:bg-black/5 disabled:opacity-50`}
        aria-label="Quantity"
      />
      <button
        type="button"
        onMouseDown={() => startHold(+1)}
        onMouseUp={stopHold}
        onMouseLeave={stopHold}
        onTouchStart={(e) => { e.preventDefault(); startHold(+1); }}
        onTouchEnd={(e) => { e.preventDefault(); stopHold(); }}
        disabled={incDisabled}
        className={buttonClass}
        aria-label="Increase quantity"
      >+</button>
    </div>
  );
}
