import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import ToggleSwitch from './ToggleSwitch';
import { getProducts, getProductByIdOrSlug } from '../../services/productService';
import { isoToLocalInput, localInputToIso } from '../../lib/datetimeLocal';

const numOrNull = (s) => (s === '' || s == null ? null : Number(s));

export default function CouponFormModal({ open, mode, initial, categories, onClose, onSubmit }) {
  const isEdit = mode === 'edit';

  const [code, setCode] = useState('');
  const [type, setType] = useState('PERCENT');
  const [value, setValue] = useState('');
  const [scope, setScope] = useState('ALL');
  const [status, setStatus] = useState('ACTIVE');
  const [minOrderAmount, setMinOrderAmount] = useState('');
  const [maxDiscountAmount, setMaxDiscountAmount] = useState('');
  const [startsAt, setStartsAt] = useState('');
  const [endsAt, setEndsAt] = useState('');
  const [maxUses, setMaxUses] = useState('');
  const [maxUsesPerUser, setMaxUsesPerUser] = useState('');
  const [categoryIds, setCategoryIds] = useState([]);
  const [products, setProducts] = useState([]); // [{id, name}]

  const [productSearch, setProductSearch] = useState('');
  const [productResults, setProductResults] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!open) return;
    setCode(initial?.code ?? '');
    setType(initial?.type ?? 'PERCENT');
    setValue(initial?.value != null ? String(initial.value) : '');
    setScope(initial?.scope ?? 'ALL');
    setStatus(initial?.status ?? 'ACTIVE');
    setMinOrderAmount(initial?.minOrderAmount != null ? String(initial.minOrderAmount) : '');
    setMaxDiscountAmount(initial?.maxDiscountAmount != null ? String(initial.maxDiscountAmount) : '');
    setStartsAt(isoToLocalInput(initial?.startsAt));
    setEndsAt(isoToLocalInput(initial?.endsAt));
    setMaxUses(initial?.maxUses != null ? String(initial.maxUses) : '');
    setMaxUsesPerUser(initial?.maxUsesPerUser != null ? String(initial.maxUsesPerUser) : '');
    setCategoryIds(initial?.categoryIds ?? []);
    setProductSearch('');
    setProductResults([]);
    setError(null);

    const ids = initial?.productIds ?? [];
    if (ids.length > 0) {
      Promise.all(ids.map((id) => getProductByIdOrSlug(id).catch(() => ({ id, name: `#${id}` }))))
        .then((list) => setProducts(list.map((p) => ({ id: p.id, name: p.name }))))
        .catch(() => setProducts(ids.map((id) => ({ id, name: `#${id}` }))));
    } else {
      setProducts([]);
    }
  }, [open, initial]);

  useEffect(() => {
    if (!open || scope !== 'PRODUCT') return;
    const term = productSearch.trim();
    if (term.length < 2) { setProductResults([]); return; }
    let cancelled = false;
    const t = setTimeout(() => {
      getProducts({ search: term, size: 6 })
        .then((data) => { if (!cancelled) setProductResults(data?.content ?? []); })
        .catch(() => { if (!cancelled) setProductResults([]); });
    }, 300);
    return () => { cancelled = true; clearTimeout(t); };
  }, [productSearch, scope, open]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e) => { if (e.key === 'Escape') onClose?.(); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;

  const toggleCategory = (id) => {
    setCategoryIds((prev) => (prev.includes(id) ? prev.filter((c) => c !== id) : [...prev, id]));
  };

  const addProduct = (p) => {
    setProducts((prev) => (prev.some((x) => x.id === p.id) ? prev : [...prev, { id: p.id, name: p.name }]));
    setProductSearch('');
    setProductResults([]);
  };

  const removeProduct = (id) => setProducts((prev) => prev.filter((p) => p.id !== id));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    const v = Number(value);
    if (!(v > 0)) { setError('Value must be greater than 0.'); return; }
    if (type === 'PERCENT' && v > 100) { setError('Percent value cannot exceed 100.'); return; }
    if (!isEdit && !/^[A-Z0-9]+$/.test(code.trim())) { setError('Code must be letters/digits (e.g. SAVE10).'); return; }
    if (scope === 'CATEGORY' && categoryIds.length === 0) { setError('Pick at least one category.'); return; }
    if (scope === 'PRODUCT' && products.length === 0) { setError('Pick at least one product.'); return; }
    const startIso = localInputToIso(startsAt);
    const endIso = localInputToIso(endsAt);
    if (startIso && endIso && new Date(endIso) <= new Date(startIso)) {
      setError('End time must be after start time.'); return;
    }

    const base = {
      type,
      value: v,
      scope,
      status,
      minOrderAmount: numOrNull(minOrderAmount),
      maxDiscountAmount: numOrNull(maxDiscountAmount),
      startsAt: startIso,
      endsAt: endIso,
      maxUses: numOrNull(maxUses),
      maxUsesPerUser: numOrNull(maxUsesPerUser),
      categoryIds: scope === 'CATEGORY' ? categoryIds : [],
      productIds: scope === 'PRODUCT' ? products.map((p) => p.id) : [],
    };
    const payload = isEdit ? base : { ...base, code: code.trim() };

    setSubmitting(true);
    try {
      await onSubmit?.(payload);
      onClose?.();
    } catch (err) {
      setError(err.message || 'Save failed');
    } finally {
      setSubmitting(false);
    }
  };

  const label = 'text-[10px] font-bold tracking-[0.15em] uppercase text-black/50';
  const input = 'mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none';

  return createPortal(
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 px-4 py-6"
      role="dialog"
      aria-modal="true"
      onClick={onClose}
    >
      <form
        className="bg-white w-full max-w-2xl p-6 max-h-full overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
        onSubmit={handleSubmit}
      >
        <h3 className="font-['Anton'] text-3xl uppercase tracking-tight mb-5">
          {isEdit ? `Edit coupon · ${initial?.code}` : 'New coupon'}
        </h3>

        {error && (
          <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-3 py-2 text-xs mb-4">{error}</div>
        )}

        <div className="grid gap-4 md:grid-cols-2">
          {!isEdit && (
            <label className="block md:col-span-2">
              <span className={label}>Code</span>
              <input
                type="text"
                value={code}
                onChange={(e) => setCode(e.target.value.toUpperCase())}
                placeholder="SAVE10"
                className={`${input} font-mono`}
                required
              />
              <span className="block mt-1 text-[10px] text-black/40">Uppercase letters/digits. Immutable after creation.</span>
            </label>
          )}

          <label className="block">
            <span className={label}>Discount type</span>
            <select value={type} onChange={(e) => setType(e.target.value)} className={`${input} bg-white`}>
              <option value="PERCENT">Percent off (%)</option>
              <option value="FIXED">Fixed amount off (VND)</option>
            </select>
          </label>

          <label className="block">
            <span className={label}>{type === 'FIXED' ? 'Amount off (VND)' : 'Percent off (%)'}</span>
            <input type="number" min="0" value={value} onChange={(e) => setValue(e.target.value)}
              placeholder={type === 'FIXED' ? '50000' : '10'} className={input} required />
          </label>

          <label className="block">
            <span className={label}>Min order (VND, optional)</span>
            <input type="number" min="0" value={minOrderAmount} onChange={(e) => setMinOrderAmount(e.target.value)}
              placeholder="0" className={input} />
          </label>

          <label className="block">
            <span className={label}>Max discount (VND, optional)</span>
            <input type="number" min="0" value={maxDiscountAmount} onChange={(e) => setMaxDiscountAmount(e.target.value)}
              placeholder="No cap" className={input} disabled={type === 'FIXED'} />
          </label>

          <label className="block">
            <span className={label}>Starts at (optional)</span>
            <input type="datetime-local" value={startsAt} onChange={(e) => setStartsAt(e.target.value)} className={input} />
          </label>

          <label className="block">
            <span className={label}>Ends at (optional)</span>
            <input type="datetime-local" value={endsAt} onChange={(e) => setEndsAt(e.target.value)} className={input} />
          </label>

          <label className="block">
            <span className={label}>Max total uses (optional)</span>
            <input type="number" min="1" value={maxUses} onChange={(e) => setMaxUses(e.target.value)}
              placeholder="Unlimited" className={input} />
          </label>

          <label className="block">
            <span className={label}>Max uses per user (optional)</span>
            <input type="number" min="1" value={maxUsesPerUser} onChange={(e) => setMaxUsesPerUser(e.target.value)}
              placeholder="Unlimited" className={input} />
          </label>

          <div className="block">
            <span className={label}>Status</span>
            <div className="mt-2">
              <ToggleSwitch
                checked={status === 'ACTIVE'}
                onChange={(v) => setStatus(v ? 'ACTIVE' : 'DISABLED')}
                labelOff="Disabled"
              />
            </div>
          </div>

          <label className="block">
            <span className={label}>Applies to (scope)</span>
            <select value={scope} onChange={(e) => setScope(e.target.value)} className={`${input} bg-white`}>
              <option value="ALL">Whole order</option>
              <option value="CATEGORY">Specific categories</option>
              <option value="PRODUCT">Specific products</option>
            </select>
          </label>
        </div>

        {scope === 'CATEGORY' && (
          <div className="mt-4">
            <span className={label}>Categories</span>
            <div className="mt-1 border border-black/15 max-h-44 overflow-y-auto p-2 grid grid-cols-2 gap-1">
              {(categories ?? []).map((c) => (
                <label key={c.id} className="flex items-center gap-2 text-xs px-1 py-0.5 cursor-pointer hover:bg-black/5">
                  <input type="checkbox" checked={categoryIds.includes(c.id)} onChange={() => toggleCategory(c.id)}
                    className="accent-[#E83354]" />
                  <span className="truncate">{c.name}</span>
                </label>
              ))}
            </div>
            <span className="block mt-1 text-[10px] text-black/40">{categoryIds.length} selected</span>
          </div>
        )}

        {scope === 'PRODUCT' && (
          <div className="mt-4">
            <span className={label}>Products</span>
            <input
              type="text"
              value={productSearch}
              onChange={(e) => setProductSearch(e.target.value)}
              placeholder="Search products to add…"
              className={input}
            />
            {productResults.length > 0 && (
              <ul className="border border-black/15 border-t-0 max-h-40 overflow-y-auto">
                {productResults.map((p) => (
                  <li key={p.id}>
                    <button type="button" onClick={() => addProduct(p)}
                      className="w-full text-left text-xs px-3 py-2 hover:bg-black/5 truncate">
                      {p.name}
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {products.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1.5">
                {products.map((p) => (
                  <span key={p.id} className="inline-flex items-center gap-1.5 bg-black/8 text-xs px-2 py-1">
                    <span className="max-w-[180px] truncate">{p.name}</span>
                    <button type="button" onClick={() => removeProduct(p.id)} className="text-black/40 hover:text-[#E83354]">×</button>
                  </span>
                ))}
              </div>
            )}
            <span className="block mt-1 text-[10px] text-black/40">{products.length} selected</span>
          </div>
        )}

        <div className="flex gap-3 mt-6">
          <button type="button" onClick={onClose} disabled={submitting}
            className="flex-1 border border-black/15 text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:border-black/40 disabled:opacity-40">
            Cancel
          </button>
          <button type="submit" disabled={submitting}
            className="flex-1 bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:bg-[#E83354] transition-colors disabled:opacity-40">
            {submitting ? 'Saving...' : isEdit ? 'Save' : 'Create'}
          </button>
        </div>
      </form>
    </div>,
    document.body,
  );
}
