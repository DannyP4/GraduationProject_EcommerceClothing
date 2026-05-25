import { useEffect, useRef, useState } from 'react';

const LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'vi', label: 'Tiếng Việt' },
  { code: 'ja', label: '日本語' },
];
const LOCALE_KEY = 'app.locale';

export default function LanguageSwitcher({ tone = 'light' }) {
  const [open, setOpen] = useState(false);
  const [locale, setLocale] = useState(() => localStorage.getItem(LOCALE_KEY) || 'en');
  const wrapRef = useRef(null);
  const closeTimerRef = useRef(null);

  const cancelClose = () => {
    if (closeTimerRef.current) { clearTimeout(closeTimerRef.current); closeTimerRef.current = null; }
  };
  const scheduleClose = () => {
    cancelClose();
    closeTimerRef.current = setTimeout(() => setOpen(false), 200);
  };
  useEffect(() => () => cancelClose(), []);

  const pick = (code) => {
    setLocale(code);
    localStorage.setItem(LOCALE_KEY, code);
    setOpen(false);
    window.dispatchEvent(new CustomEvent('app:locale-change', { detail: { locale: code } }));
  };

  const current = LANGUAGES.find((l) => l.code === locale) ?? LANGUAGES[0];
  const triggerCls = tone === 'dark'
    ? 'text-white/80 hover:text-white'
    : 'text-black/70 hover:text-[#E83354]';

  return (
    <div
      ref={wrapRef}
      className="relative hidden sm:block"
      onMouseEnter={() => { cancelClose(); setOpen(true); }}
      onMouseLeave={scheduleClose}
    >
      <button
        type="button"
        aria-haspopup="true"
        aria-expanded={open}
        className={`flex items-center gap-1 text-[11px] font-bold tracking-[0.1em] uppercase transition-all hover:-translate-y-0.5 ${triggerCls}`}
      >
        <GlobeIcon />
        <span>{current.code.toUpperCase()}</span>
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
          className={`opacity-50 transition-transform ${open ? 'rotate-180' : ''}`}>
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-44 bg-white border border-black/10 shadow-xl z-50 py-1">
          {LANGUAGES.map((l) => (
            <button
              key={l.code}
              onClick={() => pick(l.code)}
              className={`w-full text-left px-4 py-2 text-sm flex items-center justify-between transition-colors ${
                l.code === locale ? 'bg-black text-white' : 'text-black/70 hover:bg-black/5 hover:text-black'
              }`}
            >
              <span>{l.label}</span>
              <span className="text-[10px] opacity-60">{l.code.toUpperCase()}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function GlobeIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <circle cx="12" cy="12" r="10" />
      <line x1="2" y1="12" x2="22" y2="12" />
      <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
    </svg>
  );
}
