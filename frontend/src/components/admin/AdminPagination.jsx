import { useState } from 'react';

const ARROW_BTN =
  'inline-flex items-center gap-1.5 text-[11px] font-bold tracking-[0.15em] uppercase border-2 border-black px-4 py-2 hover:bg-black hover:text-white transition-colors disabled:border-black/30 disabled:text-black/30 disabled:hover:bg-transparent disabled:hover:text-black/30 disabled:cursor-not-allowed';

export default function AdminPagination({ page, totalPages, totalElements, onChange }) {
  const [goto, setGoto] = useState('');
  if (totalPages <= 1) return null;

  const jump = (e) => {
    e.preventDefault();
    const n = Number(goto);
    if (Number.isInteger(n) && n >= 1 && n <= totalPages) onChange(n - 1);
    setGoto('');
  };

  return (
    <div className="flex items-center justify-between flex-wrap gap-3">
      <span className="text-xs text-black/70">
        Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong>{' '}
        <span className="text-black/40">({totalElements} total)</span>
      </span>
      <div className="flex items-center gap-2">
        <form onSubmit={jump} className="mr-1">
          <input
            type="number"
            min="1"
            max={totalPages}
            value={goto}
            onChange={(e) => setGoto(e.target.value)}
            placeholder="Go to page…"
            aria-label="Go to page"
            title="Type a page number and press Enter"
            className="w-24 border border-black/15 px-2 py-2 text-xs focus:border-black focus:outline-none"
          />
        </form>
        <button type="button" onClick={() => onChange(Math.max(0, page - 1))} disabled={page === 0} className={ARROW_BTN}>
          <span>&larr;</span> Prev
        </button>
        <button type="button" onClick={() => onChange(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1} className={ARROW_BTN}>
          Next <span>&rarr;</span>
        </button>
      </div>
    </div>
  );
}
