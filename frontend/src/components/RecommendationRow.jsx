import { useCallback, useEffect, useState } from 'react';
import ProductCard from './ProductCard';
import useAutoHideScrollbar from '../lib/useAutoHideScrollbar';

function Chevron({ dir }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
      {dir < 0 ? <polyline points="15 18 9 12 15 6" /> : <polyline points="9 18 15 12 9 6" />}
    </svg>
  );
}

// rendered only when there are items -> scroll ref is attached on mount.
export function Carousel({ title, items }) {
  const scrollRef = useAutoHideScrollbar();
  const [canLeft, setCanLeft] = useState(false);
  const [canRight, setCanRight] = useState(false);

  const update = useCallback(() => {
    const el = scrollRef.current;
    if (!el) return;
    setCanLeft(el.scrollLeft > 4);
    setCanRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 4);
  }, [scrollRef]);

  useEffect(() => {
    update();
    const el = scrollRef.current;
    if (!el) return undefined;
    el.addEventListener('scroll', update, { passive: true });
    window.addEventListener('resize', update);
    return () => {
      el.removeEventListener('scroll', update);
      window.removeEventListener('resize', update);
    };
  }, [update, items]);

  const scrollByCards = (dir) => {
    const el = scrollRef.current;
    if (el) el.scrollBy({ left: dir * el.clientWidth * 0.8, behavior: 'smooth' });
  };

  return (
    <section className="mt-12">
      <div className="flex items-end justify-between mb-5">
        <h2 className="font-['Anton'] text-2xl md:text-3xl uppercase tracking-tight">{title}</h2>
        {(canLeft || canRight) && (
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => scrollByCards(-1)}
              disabled={!canLeft}
              aria-label="Scroll left"
              className="w-9 h-9 flex items-center justify-center border border-black/20 text-black hover:bg-black hover:text-white transition-colors disabled:opacity-25 disabled:hover:bg-transparent disabled:hover:text-black"
            >
              <Chevron dir={-1} />
            </button>
            <button
              type="button"
              onClick={() => scrollByCards(1)}
              disabled={!canRight}
              aria-label="Scroll right"
              className="w-9 h-9 flex items-center justify-center border border-black/20 text-black hover:bg-black hover:text-white transition-colors disabled:opacity-25 disabled:hover:bg-transparent disabled:hover:text-black"
            >
              <Chevron dir={1} />
            </button>
          </div>
        )}
      </div>
      <div
        ref={scrollRef}
        className="flex gap-4 overflow-x-auto scrollbar-subtle snap-x snap-mandatory pb-3"
      >
        {items.map((p) => (
          <div key={p.id} className="flex-shrink-0 w-[150px] sm:w-[190px] snap-start">
            <ProductCard product={p} />
          </div>
        ))}
      </div>
    </section>
  );
}

export default function RecommendationRow({ title, productId, fetcher }) {
  const [items, setItems] = useState(null);

  useEffect(() => {
    if (!productId) return undefined;
    let cancelled = false;
    setItems(null);
    fetcher(productId)
      .then((data) => { if (!cancelled) setItems(data || []); })
      .catch(() => { if (!cancelled) setItems([]); });
    return () => { cancelled = true; };
  }, [productId, fetcher]);

  if (!items || items.length === 0) return null;
  return <Carousel title={title} items={items} />;
}
