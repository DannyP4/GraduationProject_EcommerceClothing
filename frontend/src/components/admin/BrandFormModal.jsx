import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import ToggleSwitch from './ToggleSwitch';
import CollapsibleSection from './CollapsibleSection';

export default function BrandFormModal({ open, mode, initial, onClose, onSubmit }) {
  const [slug, setSlug] = useState('');
  const [name, setName] = useState('');
  const [logoUrl, setLogoUrl] = useState('');
  const [websiteUrl, setWebsiteUrl] = useState('');
  const [descriptionEn, setDescriptionEn] = useState('');
  const [descriptionVi, setDescriptionVi] = useState('');
  const [descriptionJa, setDescriptionJa] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) return;
    setSlug(initial?.slug ?? '');
    setName(initial?.name ?? '');
    setLogoUrl(initial?.logoUrl ?? '');
    setWebsiteUrl(initial?.websiteUrl ?? '');
    setDescriptionEn(initial?.descriptionEn ?? '');
    setDescriptionVi(initial?.descriptionVi ?? '');
    setDescriptionJa(initial?.descriptionJa ?? '');
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
      setError('Slug must be lowercase kebab-case (e.g. atlas-studio)');
      return;
    }
    setSubmitting(true);
    try {
      const descriptions = { descriptionEn, descriptionVi, descriptionJa };
      const payload = isEdit
        ? { name, logoUrl, websiteUrl, isActive, ...descriptions }
        : { slug, name, logoUrl, websiteUrl, isActive, ...descriptions };
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
          {isEdit ? 'Edit brand' : 'New brand'}
        </h3>

        {error && (
          <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-3 py-2 text-xs mb-4">
            {error}
          </div>
        )}

        {!isEdit && (
          <label className="block mb-3">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Slug</span>
            <input
              type="text"
              value={slug}
              onChange={(e) => setSlug(e.target.value.toLowerCase())}
              placeholder="atlas-studio"
              className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
              required
            />
            <span className="block mt-1 text-[10px] text-black/40">Permanent identifier, lowercase with dashes.</span>
          </label>
        )}

        <label className="block mb-3">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Name</span>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Atlas Studio"
            className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
            required
          />
        </label>

        <label className="block mb-3">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Logo URL</span>
          <input
            type="url"
            value={logoUrl}
            onChange={(e) => setLogoUrl(e.target.value)}
            placeholder="https://res.cloudinary.com/..."
            className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
          />
          <span className="block mt-1 text-[10px] text-black/40">Paste a hosted image URL (e.g. Cloudinary).</span>
        </label>

        <label className="block mb-4">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Website</span>
          <input
            type="url"
            value={websiteUrl}
            onChange={(e) => setWebsiteUrl(e.target.value)}
            placeholder="https://atlas-studio.example.com"
            className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
          />
        </label>

        <label className="block mb-4">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Description (English)</span>
          <textarea
            value={descriptionEn}
            onChange={(e) => setDescriptionEn(e.target.value)}
            rows={3}
            placeholder="Atlas Studio crafts minimalist everyday essentials."
            className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none resize-y"
          />
        </label>

        <div className="mb-5">
          <CollapsibleSection
            title="Translations"
            badge={(descriptionVi || descriptionJa) ? <span className="inline-block w-1.5 h-1.5 rounded-full bg-[#E83354]" title="Has translations" /> : null}
          >
            <label className="block mb-3">
              <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Description · Tiếng Việt</span>
              <textarea
                value={descriptionVi}
                onChange={(e) => setDescriptionVi(e.target.value)}
                rows={3}
                className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none resize-y"
              />
            </label>
            <label className="block">
              <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Description · 日本語</span>
              <textarea
                value={descriptionJa}
                onChange={(e) => setDescriptionJa(e.target.value)}
                rows={3}
                className="mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none resize-y"
              />
            </label>
          </CollapsibleSection>
        </div>

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
