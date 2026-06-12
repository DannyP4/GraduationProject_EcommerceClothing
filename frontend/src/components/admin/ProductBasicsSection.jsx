import ToggleSwitch from './ToggleSwitch';
import CollapsibleSection from './CollapsibleSection';

const GENDER_OPTIONS = ['MEN', 'WOMEN', 'UNISEX', 'KIDS'];

export default function ProductBasicsSection({
  isCreate,
  readOnly = false,
  values,
  setValues,
  brands,
  categories,
  error,
}) {
  const update = (patch) => setValues((v) => ({ ...v, ...patch }));
  const disabledCls = 'disabled:bg-black/5 disabled:text-black/50';

  const salePreview = (() => {
    const base = Number(values.basePrice);
    const v = Number(values.saleValue);
    if (!values.saleType || !(base > 0) || !(v > 0)) return null;
    const raw = values.saleType === 'PERCENT' ? base * (1 - v / 100) : base - v;
    return new Intl.NumberFormat('vi-VN').format(Math.max(0, Math.round(raw))) + ' ₫';
  })();

  return (
    <section className="bg-white border border-black/10 p-6 space-y-5">
      <h2 className="font-['Anton'] text-2xl uppercase tracking-tight">Basics</h2>

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-3 py-2 text-xs">{error}</div>
      )}

      <div className="grid gap-5 md:grid-cols-2">
        <label className="block">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Slug</span>
          <input
            type="text"
            value={values.slug}
            onChange={(e) => update({ slug: e.target.value.toLowerCase() })}
            placeholder="essential-tee-black"
            disabled={!isCreate || readOnly}
            className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none ${disabledCls}`}
          />
          <span className="block mt-1 text-[10px] text-black/40">
            {isCreate ? 'Permanent identifier, lowercase with dashes.' : 'Slug is immutable after creation.'}
          </span>
        </label>

        <label className="block">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Name (English)</span>
          <input
            type="text"
            value={values.name}
            onChange={(e) => update({ name: e.target.value })}
            placeholder="Essential Tee"
            disabled={readOnly}
            className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none ${disabledCls}`}
          />
        </label>

        <label className="block">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Brand</span>
          <select
            value={values.brandId}
            onChange={(e) => update({ brandId: e.target.value })}
            disabled={readOnly}
            className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white ${disabledCls}`}
          >
            <option value="">Select brand</option>
            {brands.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
        </label>

        <label className="block">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Category</span>
          <select
            value={values.categoryId}
            onChange={(e) => update({ categoryId: e.target.value })}
            disabled={readOnly}
            className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white ${disabledCls}`}
          >
            <option value="">Select category</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>

        <label className="block">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Gender</span>
          <select
            value={values.gender}
            onChange={(e) => update({ gender: e.target.value })}
            disabled={readOnly}
            className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white ${disabledCls}`}
          >
            {GENDER_OPTIONS.map((g) => <option key={g} value={g}>{g}</option>)}
          </select>
        </label>

        <label className="block">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Base price (VND)</span>
          <input
            type="number"
            min="0"
            step="1000"
            value={values.basePrice}
            onChange={(e) => update({ basePrice: e.target.value })}
            placeholder="250000"
            disabled={readOnly}
            className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none ${disabledCls}`}
          />
        </label>
      </div>

      <div className="border-t border-black/10 pt-5">
        <div className="flex items-center justify-between mb-3">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Sale (optional)</span>
          {salePreview && (
            <span className="text-[11px] font-bold text-[#E83354]">Sale price → {salePreview}</span>
          )}
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <label className="block">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Discount type</span>
            <select
              value={values.saleType}
              onChange={(e) => update({ saleType: e.target.value })}
              disabled={readOnly}
              className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white ${disabledCls}`}
            >
              <option value="">No sale</option>
              <option value="PERCENT">Percent off (%)</option>
              <option value="FIXED">Fixed amount off (VND)</option>
            </select>
          </label>

          <label className="block">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">
              {values.saleType === 'FIXED' ? 'Amount off (VND)' : 'Percent off (%)'}
            </span>
            <input
              type="number"
              min="0"
              step={values.saleType === 'FIXED' ? '1000' : '1'}
              value={values.saleValue}
              onChange={(e) => update({ saleValue: e.target.value })}
              placeholder={values.saleType === 'FIXED' ? '50000' : '30'}
              disabled={readOnly || !values.saleType}
              className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none ${disabledCls}`}
            />
          </label>

          <label className="block">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Starts at (optional)</span>
            <input
              type="datetime-local"
              value={values.saleStartsAt}
              onChange={(e) => update({ saleStartsAt: e.target.value })}
              disabled={readOnly || !values.saleType}
              className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none ${disabledCls}`}
            />
          </label>

          <label className="block">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Ends at (optional)</span>
            <input
              type="datetime-local"
              value={values.saleEndsAt}
              onChange={(e) => update({ saleEndsAt: e.target.value })}
              disabled={readOnly || !values.saleType}
              className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none ${disabledCls}`}
            />
          </label>
        </div>
        <p className="mt-2 text-[10px] text-black/40">Leave both dates empty for an always-on sale. End time is exclusive.</p>
      </div>

      <label className="block">
        <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Description (English)</span>
        <textarea
          value={values.description}
          onChange={(e) => update({ description: e.target.value })}
          rows={4}
          disabled={readOnly}
          className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none resize-y ${disabledCls}`}
        />
      </label>

      <CollapsibleSection
        title="Translations"
        badge={(values.nameVi || values.nameJa || values.descriptionVi || values.descriptionJa)
          ? <span className="inline-block w-1.5 h-1.5 rounded-full bg-[#E83354]" title="Has translations" />
          : null}
      >
        <div className="grid gap-4 md:grid-cols-2">
          <label className="block">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Name · Tiếng Việt</span>
            <input
              type="text"
              value={values.nameVi}
              onChange={(e) => update({ nameVi: e.target.value })}
              placeholder="Áo thun"
              disabled={readOnly}
              className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none ${disabledCls}`}
            />
          </label>
          <label className="block">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Name · 日本語</span>
            <input
              type="text"
              value={values.nameJa}
              onChange={(e) => update({ nameJa: e.target.value })}
              placeholder="Tシャツ"
              disabled={readOnly}
              className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none ${disabledCls}`}
            />
          </label>
          <label className="block md:col-span-2">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Description · Tiếng Việt</span>
            <textarea
              value={values.descriptionVi}
              onChange={(e) => update({ descriptionVi: e.target.value })}
              rows={3}
              disabled={readOnly}
              className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none resize-y ${disabledCls}`}
            />
          </label>
          <label className="block md:col-span-2">
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Description · 日本語</span>
            <textarea
              value={values.descriptionJa}
              onChange={(e) => update({ descriptionJa: e.target.value })}
              rows={3}
              disabled={readOnly}
              className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none resize-y ${disabledCls}`}
            />
          </label>
        </div>
      </CollapsibleSection>

      <div>
        <ToggleSwitch checked={values.isActive} onChange={(v) => update({ isActive: v })} disabled={readOnly} />
      </div>
    </section>
  );
}
