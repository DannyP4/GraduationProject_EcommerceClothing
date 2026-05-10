import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';

const ToastContext = createContext(null);
const DEFAULT_DURATION_MS = 3000;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const idRef = useRef(0);

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const show = useCallback((opts) => {
    const id = ++idRef.current;
    const toast = {
      id,
      type: opts.type ?? 'success',
      message: opts.message ?? '',
      duration: opts.duration ?? DEFAULT_DURATION_MS,
    };
    setToasts((prev) => [...prev, toast]);
    return id;
  }, []);

  const api = {
    show,
    success: (message, opts = {}) => show({ ...opts, type: 'success', message }),
    error: (message, opts = {}) => show({ ...opts, type: 'error', message }),
    info: (message, opts = {}) => show({ ...opts, type: 'info', message }),
    dismiss,
  };

  return (
    <ToastContext.Provider value={api}>
      {children}
      {createPortal(
        <div className="fixed top-20 right-4 z-[200] flex flex-col gap-2 pointer-events-none">
          {toasts.map((t) => (
            <ToastItem key={t.id} toast={t} onDismiss={() => dismiss(t.id)} />
          ))}
        </div>,
        document.body
      )}
    </ToastContext.Provider>
  );
}

function ToastItem({ toast, onDismiss }) {
  const [leaving, setLeaving] = useState(false);

  useEffect(() => {
    const enter = setTimeout(() => { }, 10);
    const start = setTimeout(() => setLeaving(true), Math.max(toast.duration - 200, 100));
    const end = setTimeout(onDismiss, toast.duration);
    return () => { clearTimeout(enter); clearTimeout(start); clearTimeout(end); };
  }, [toast.duration, onDismiss]);

  const palette = {
    success: 'bg-white border-l-4 border-green-600 text-black',
    error: 'bg-white border-l-4 border-[#E83354] text-black',
    info: 'bg-white border-l-4 border-black text-black',
  }[toast.type];

  const icon = {
    success: <CheckIcon className="text-green-600" />,
    error: <XIcon className="text-[#E83354]" />,
    info: <InfoIcon className="text-black/70" />,
  }[toast.type];

  return (
    <div
      className={`pointer-events-auto min-w-[280px] max-w-sm shadow-lg flex items-start gap-3 px-4 py-3 transition-all duration-200 ${palette} ${leaving ? 'opacity-0 translate-x-2' : 'opacity-100 translate-x-0'
        }`}
      role="status"
    >
      <span className="flex-shrink-0 mt-0.5">{icon}</span>
      <p className="flex-1 text-xs font-medium leading-relaxed">{toast.message}</p>
      <button
        onClick={() => { setLeaving(true); setTimeout(onDismiss, 200); }}
        className="flex-shrink-0 text-black/30 hover:text-black transition-colors"
        aria-label="Close"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}

function CheckIcon({ className = '' }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" className={className}>
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}
function XIcon({ className = '' }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" className={className}>
      <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}
function InfoIcon({ className = '' }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" className={className}>
      <circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" />
    </svg>
  );
}
