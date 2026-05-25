import { useEffect } from 'react';
import { createPortal } from 'react-dom';

export default function ConfirmDialog({
  open,
  title = 'Are you sure?',
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  tone = 'default',
  hideCancel = false,
  onConfirm,
  onCancel,
}) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e) => { if (e.key === 'Escape') onCancel?.(); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onCancel]);

  if (!open) return null;

  const confirmCls =
    tone === 'danger'
      ? 'bg-[#E83354] text-white hover:bg-[#c82244]'
      : 'bg-black text-white hover:bg-[#E83354]';

  // Portal to body so the modal isn't trapped by an ancestor's stacking context / containing block
  return createPortal(
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 px-4"
      role="dialog"
      aria-modal="true"
      onClick={onCancel}
    >
      <div
        className="bg-white max-w-sm w-full p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 className="font-['Anton'] text-2xl uppercase tracking-tight mb-2">{title}</h3>
        {message && <p className="text-sm text-black/60 mb-6">{message}</p>}
        <div className="flex gap-3">
          {!hideCancel && (
            <button
              type="button"
              onClick={onCancel}
              className="flex-1 border border-black/15 text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:border-black/40 transition-all"
            >
              {cancelLabel}
            </button>
          )}
          <button
            type="button"
            onClick={onConfirm}
            className={`flex-1 text-[11px] font-bold tracking-[0.15em] uppercase py-3 transition-colors ${confirmCls}`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
