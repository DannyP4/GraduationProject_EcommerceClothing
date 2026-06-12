import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import ToggleSwitch from './ToggleSwitch';
import CollapsibleSection from './CollapsibleSection';

export default function CategoryFormModal({ open, mode, initial, onClose, onSubmit }) {
  const [slug, setSlug] = useState('');
  const [name, setName] = useState('');
  const [nameVi, setNameVi] = useState('');
  const [nameJa, setNameJa] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) return;
    setSlug(initial?.slug ?? '');
    setName(initial?.name ?? '');
    setNameVi(initial?.nameVi ?? '');
    setNameJa(initial?.nameJa ?? '');
    setImageUrl(initial?.imageUrl ?? '');
    setIsActive(initial?.isActive ?? true);
    setError(null);
  }, [open, initial]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e) => { if (e.key === 'Escape') onClose?.(); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;

  const isEdit = mode === 'edit';

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!name.trim()) { setError('Name is required'); return; }
    if (!isEdit && !/^[a-z0-9]+(-[a-z0-9]+)*$/.test(slug)) {
      setError('Slug must be lowercase kebab-case (e.g. t-shirts)');
      return;
    }
    setSubmitting(true);
    try {
      const payload = isEdit
        ? { name, nameVi, nameJa, imageUrl, isActive }
        : { slug, name, nameVi, nameJa, imageUrl, isActive };
      await onSubmit?.(payload);
      onClose?.();
    } catch (err) {
      setError(err.message || 'Save failed');
    } finally {
      setSubmitting(false);
    }
  };

  return createPortal(
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 px-4 py-6"
      role="dialog"
      aria-modal="true"
      onClick={onClose}
    >
      <form
        className="bg-white w-full max-w-lg p-6 max-h-full overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
        onSubmit={handleSubmit}
      >
        <h3 className="font-['Anton'] text-3xl uppercase tracking-tight mb-5">
          {isEdit ? 'Edit category' : 'New category'}
        </h3>

        {error && (
          <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-3 py-2 text-xs mb-4">
            {error}
          </div>
        )}

        {!isEdit && (
          <label className="block mb-4">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Slug</span>
            <input
              type="text"
              value={slug}
              onChange={(e) => setSlug(e.target.value.toLowerCase())}
              placeholder="t-shirts"
              className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
              required
            />
            <span className="block mt-1 text-[10px] text-black/40">Permanent identifier, lowercase with dashes.</span>
          </label>
        )}

        <label className="block mb-4">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Name (English)</span>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="T-Shirts"
            className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
            required
          />
        </label>

        <div className="mb-5">
          <CollapsibleSection
            title="Translations"
            badge={(nameVi || nameJa) ? <span className="inline-block w-1.5 h-1.5 rounded-full bg-[#E83354]" title="Has translations" /> : null}
          >
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block">
              <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Name · Tiếng Việt</span>
              <input
                type="text"
                value={nameVi}
                onChange={(e) => setNameVi(e.target.value)}
                placeholder="Áo thun"
                className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
              />
            </label>
            <label className="block">
              <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Name · 日本語</span>
              <input
                type="text"
                value={nameJa}
                onChange={(e) => setNameJa(e.target.value)}
                placeholder="Tシャツ"
                className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
              />
            </label>
          </div>
          </CollapsibleSection>
        </div>

        <label className="block mb-5">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Image URL</span>
          <input
            type="url"
            value={imageUrl}
            onChange={(e) => setImageUrl(e.target.value)}
            placeholder="https://res.cloudinary.com/..."
            className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
          />
          <span className="block mt-1 text-[10px] text-black/40">Storefront tile / banner image. Paste a hosted image URL (e.g. Cloudinary).</span>
        </label>

        <div className="mb-6">
          <ToggleSwitch checked={isActive} onChange={setIsActive} />
        </div>

        <div className="flex gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="flex-1 border border-black/15 text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:border-black/40 disabled:opacity-40"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="flex-1 bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:bg-[#E83354] transition-colors disabled:opacity-40"
          >
            {submitting ? 'Saving...' : isEdit ? 'Save' : 'Create'}
          </button>
        </div>
      </form>
    </div>,
    document.body,
  );
}
