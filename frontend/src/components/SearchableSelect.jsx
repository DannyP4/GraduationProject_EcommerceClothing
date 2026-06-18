import { useMemo, useRef, useState } from 'react';

function normalize(s) {
  return (s ?? '').toString().normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
}

export default function SearchableSelect({
  label, required, value, onChange, options,
  placeholder, loading, loadingPlaceholder, disabled,
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const blurTimer = useRef(null);

  const selectedLabel = useMemo(
    () => options.find((o) => String(o.value) === String(value))?.label ?? '',
    [options, value],
  );

  const filtered = useMemo(() => {
    if (!open) return [];
    const q = normalize(query);
    const list = q ? options.filter((o) => normalize(o.label).includes(q)) : options;
    return list.slice(0, 100);
  }, [open, query, options]);

  const choose = (opt) => {
    onChange(opt.value);
    setQuery('');
    setOpen(false);
  };

  return (
    <div className="relative">
      <label className="block text-[10px] font-bold tracking-[0.15em] uppercase text-black/50 mb-1.5">
        {label}{required && <span className="text-[#E83354]"> *</span>}
      </label>
      <input
        type="text"
        autoComplete="off"
        disabled={disabled}
        value={open ? query : selectedLabel}
        placeholder={loading ? loadingPlaceholder : placeholder}
        onFocus={() => { if (blurTimer.current) clearTimeout(blurTimer.current); setQuery(''); setOpen(true); }}
        onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
        onBlur={() => { blurTimer.current = setTimeout(() => setOpen(false), 150); }}
        className="w-full border border-black/15 px-3 py-2.5 text-sm bg-white focus:outline-none focus:border-black transition-colors disabled:bg-black/5 disabled:text-black/40"
      />
      {open && filtered.length > 0 && (
        <ul className="absolute z-20 left-0 right-0 mt-1 max-h-56 overflow-y-auto bg-white border border-black/15 shadow-lg">
          {filtered.map((o) => (
            <li key={o.value}>
              <button
                type="button"
                onMouseDown={(e) => { e.preventDefault(); choose(o); }}
                className={`w-full text-left px-3 py-2 text-sm hover:bg-black/5 ${
                  String(o.value) === String(value) ? 'font-bold text-[#E83354]' : ''
                }`}
              >
                {o.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
