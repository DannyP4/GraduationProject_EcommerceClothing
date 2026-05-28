import ToggleSwitch from './ToggleSwitch';

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
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Name</span>
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

      <label className="block">
        <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/50">Description</span>
        <textarea
          value={values.description}
          onChange={(e) => update({ description: e.target.value })}
          rows={4}
          disabled={readOnly}
          className={`mt-1 w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none resize-y ${disabledCls}`}
        />
      </label>

      <div>
        <ToggleSwitch checked={values.isActive} onChange={(v) => update({ isActive: v })} disabled={readOnly} />
      </div>
    </section>
  );
}
