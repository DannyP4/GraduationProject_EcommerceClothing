import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import { getProducts, getCategories } from '../services/productService';
import useScrollRestore from '../lib/useScrollRestore';
import useAutoHideScrollbar from '../lib/useAutoHideScrollbar';

const SORT_OPTIONS = [
  { label: 'Newest', value: 'NEWEST' },
  { label: 'Price: Low to High', value: 'PRICE_ASC' },
  { label: 'Price: High to Low', value: 'PRICE_DESC' },
  { label: 'Popular', value: 'POPULAR' },
];

const PAGE_SIZE = 12;

export default function ShopPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const query = searchParams.get('q')?.trim() ?? '';

  const [categories, setCategories] = useState([]);
  const [catFilter, setCatFilter] = useState('');
  const catScrollRef = useAutoHideScrollbar();
  const [activeCategoryId, setActiveCategoryId] = useState(() => {
    const n = Number(searchParams.get('category'));
    return Number.isFinite(n) && n > 0 ? n : null;
  });
  const [sort, setSort] = useState(() => searchParams.get('sort') ?? 'NEWEST');
  const [page, setPage] = useState(() => Math.max(0, Math.floor(Number(searchParams.get('page')) || 1) - 1));

  const [items, setItems] = useState([]);
  const [meta, setMeta] = useState({ totalElements: 0, totalPages: 0, hasNext: false });
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);
  const [reloadNonce, setReloadNonce] = useState(0);

  const prevQuery = useRef(query);
  useEffect(() => {
    if (prevQuery.current !== query) {
      prevQuery.current = query;
      setPage(0);
    }
  }, [query]);

  useEffect(() => {
    const next = new URLSearchParams();
    if (query) next.set('q', query);
    if (activeCategoryId != null) next.set('category', String(activeCategoryId));
    if (sort !== 'NEWEST') next.set('sort', sort);
    if (page > 0) next.set('page', String(page + 1));
    if (next.toString() !== searchParams.toString()) {
      setSearchParams(next, { replace: true });
    }
  }, [query, activeCategoryId, sort, page, searchParams, setSearchParams]);

  useEffect(() => {
    let cancelled = false;
    getCategories()
      .then((data) => { if (!cancelled) setCategories(data || []); })
      .catch(() => { });
    return () => { cancelled = true; };
  }, []);

  const loaded = useRef({ key: null, through: -1 });
  useEffect(() => {
    let cancelled = false;
    const filterKey = `${activeCategoryId ?? ''}|${sort}|${query}`;
    const fetchPage = (p) => getProducts({
      page: p,
      size: PAGE_SIZE,
      sort,
      categoryId: activeCategoryId ?? undefined,
      search: query || undefined,
    });
    const prev = loaded.current;
    const sameFilter = prev.key === filterKey;
    if (sameFilter && page <= prev.through) return;

    const run = async () => {
      try {
        if (sameFilter && page === prev.through + 1) {
          setLoadingMore(true);
          const data = await fetchPage(page);
          if (cancelled) return;
          setItems((cur) => [...cur, ...(data?.content ?? [])]);
          setMeta({ totalElements: data?.totalElements ?? 0, totalPages: data?.totalPages ?? 0, hasNext: !!data?.hasNext });
        } else {
          setLoading(true);
          const results = await Promise.all(
            Array.from({ length: page + 1 }, (_, p) => fetchPage(p))
          );
          if (cancelled) return;
          const last = results[results.length - 1];
          setItems(results.flatMap((r) => r?.content ?? []));
          setMeta({ totalElements: last?.totalElements ?? 0, totalPages: last?.totalPages ?? 0, hasNext: !!last?.hasNext });
        }
        loaded.current = { key: filterKey, through: page };
        setError(null);
      } catch (err) {
        if (!cancelled) setError(err.message || 'Failed to load products');
      } finally {
        if (!cancelled) { setLoading(false); setLoadingMore(false); }
      }
    };
    run();
    return () => { cancelled = true; };
  }, [page, sort, activeCategoryId, query, reloadNonce]);

  const clearQuery = () => {
    const next = new URLSearchParams(searchParams);
    next.delete('q');
    setSearchParams(next, { replace: true });
  };

  const products = items;
  const totalElements = meta.totalElements;
  const hasNext = meta.hasNext;

  useScrollRestore(!loading && products.length > 0);

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />

      {/* Page header */}
      <div className="bg-[#0A0A0A] text-white py-12 px-6">
        <div className="max-w-[1440px] mx-auto">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-white/40 mb-2">
            {query
              ? `Search · ${loading ? '…' : `${totalElements} result${totalElements === 1 ? '' : 's'}`}`
              : `SS 2026 · ${loading ? '…' : `${totalElements} items`}`}
          </p>
          <h1 className="font-['Anton'] text-5xl md:text-7xl tracking-tight uppercase">
            {query ? <>Results for &ldquo;{query}&rdquo;</> : 'Shop All'}
          </h1>
          {query && (
            <button
              onClick={clearQuery}
              className="mt-3 inline-flex items-center gap-2 text-[10px] font-bold tracking-[0.15em] uppercase text-white/60 hover:text-[#E83354] transition-colors"
            >
              <span>Clear search</span>
              <span aria-hidden>×</span>
            </button>
          )}
        </div>
      </div>

      <div className="max-w-[1440px] mx-auto px-6 py-10 flex gap-8">
        {/* SIDEBAR */}
        <aside className="w-56 flex-shrink-0">
          <div className="sticky top-20 space-y-8">
            {/* Categories */}
            <div>
              <h3 className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-3">Collections</h3>
              <input
                type="text"
                value={catFilter}
                onChange={(e) => setCatFilter(e.target.value)}
                placeholder="Search category…"
                className="w-full border border-black/15 bg-white text-xs px-2.5 py-1.5 mb-2 focus:outline-none focus:border-black"
              />
              <ul ref={catScrollRef} className="space-y-1 max-h-[52vh] overflow-y-auto pr-1 scrollbar-subtle">
                <li>
                  <button
                    onClick={() => { setActiveCategoryId(null); setPage(0); }}
                    className={`w-full text-left text-sm py-1.5 px-2 transition-all font-medium ${activeCategoryId === null
                      ? 'bg-black text-white'
                      : 'text-black/60 hover:text-black hover:bg-black/5'
                      }`}
                  >
                    All
                  </button>
                </li>
                {categories
                  .filter((c) => c.name.toLowerCase().includes(catFilter.trim().toLowerCase()))
                  .map((c) => (
                  <li key={c.id}>
                    <button
                      onClick={() => { setActiveCategoryId(c.id); setPage(0); }}
                      className={`w-full text-left text-sm py-1.5 px-2 transition-all font-medium ${activeCategoryId === c.id
                        ? 'bg-black text-white'
                        : 'text-black/60 hover:text-black hover:bg-black/5'
                        }`}
                    >
                      {c.name}
                    </button>
                  </li>
                ))}
              </ul>
            </div>

            {/* Sort */}
            <div>
              <h3 className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-4">Sort By</h3>
              <select
                value={sort}
                onChange={(e) => { setSort(e.target.value); setPage(0); }}
                className="w-full border border-black/15 bg-white text-sm px-3 py-2.5 focus:outline-none focus:border-black appearance-none cursor-pointer"
              >
                {SORT_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </div>

            {activeCategoryId !== null && (
              <button
                onClick={() => { setActiveCategoryId(null); setPage(0); }}
                className="text-[11px] font-bold tracking-[0.1em] uppercase text-[#E83354] hover:underline"
              >
                Clear Filters
              </button>
            )}
          </div>
        </aside>

        {/* PRODUCT GRID */}
        <main className="flex-1">
          {error ? (
            <div className="bg-white border border-[#E83354]/30 px-6 py-10 text-center">
              <p className="text-sm font-bold text-[#E83354] mb-1 uppercase tracking-wider">Could not load products</p>
              <p className="text-xs text-black/60 mb-4">{error}</p>
              <button
                onClick={() => { loaded.current = { key: null, through: -1 }; setError(null); setReloadNonce((n) => n + 1); }}
                className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
              >
                Retry
              </button>
            </div>
          ) : loading ? (
            <SkeletonGrid count={PAGE_SIZE} />
          ) : products.length === 0 ? (
            query ? (
              <div className="bg-white border border-black/10 py-20 px-6 text-center">
                <p className="font-['Anton'] text-4xl uppercase tracking-tight mb-3">
                  No matches for &ldquo;{query}&rdquo;
                </p>
                <p className="text-sm text-black/60 mb-8">
                  We couldn&rsquo;t find anything matching that search. Try a different keyword or browse the full catalog.
                </p>
                <button
                  onClick={clearQuery}
                  className="inline-block bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase px-12 py-4 hover:bg-[#E83354] transition-colors"
                >
                  Back to Shop
                </button>
              </div>
            ) : (
              <div className="text-center py-24 text-black/40">
                <p className="text-xl font-bold mb-2">No products found</p>
                <p className="text-sm">Try adjusting your filters</p>
              </div>
            )
          ) : (
            <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
              {products.map((product) => (
                <div
                  key={product.id}
                  className="group bg-white cursor-pointer relative overflow-hidden transition-all duration-300 ease-out hover:-translate-y-1 hover:shadow-[0_12px_24px_-12px_rgba(0,0,0,0.25)]"
                  onClick={() => navigate(`/product/${product.slug || product.id}`, { state: { backTo: `/shop${location.search}` } })}
                >
                  <div className="relative overflow-hidden" style={{ paddingTop: '125%' }}>
                    {product.discountPercent != null && (
                      <span className="absolute top-2 left-2 z-10 bg-[#E83354] text-white text-[10px] font-bold tracking-widest uppercase px-2 py-1">
                        -{product.discountPercent}%
                      </span>
                    )}
                    {product.primaryImageUrl ? (
                      <img
                        src={product.primaryImageUrl}
                        alt={product.name}
                        className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                      />
                    ) : (
                      <div className="absolute inset-0 bg-black/5 flex items-center justify-center text-black/30 text-xs">
                        No image
                      </div>
                    )}
                    {/* Slide-up overlay — translate-y-full hidden, rises on hover */}
                    <div className="absolute inset-x-0 bottom-0 bg-black/85 text-white text-center py-3 translate-y-full group-hover:translate-y-0 transition-transform duration-300 ease-out">
                      <span className="text-[11px] font-bold tracking-[0.2em] uppercase">View Details →</span>
                    </div>
                  </div>

                  <div className="p-4">
                    <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 mb-1">
                      {product.categoryName}
                    </p>
                    <h3 className="text-sm font-bold uppercase tracking-wider mb-2 line-clamp-2 min-h-[2.5rem] group-hover:text-[#E83354] transition-colors">{product.name}</h3>
                    <div className="flex items-baseline gap-2">
                      {product.salePrice != null ? (
                        <>
                          <span className="font-['Anton'] text-xl text-[#E83354]">
                            {formatPrice(product.salePrice, product.currency)}
                          </span>
                          <span className="text-xs text-black/40 line-through">
                            {formatPrice(product.basePrice, product.currency)}
                          </span>
                        </>
                      ) : (
                        <span className="font-['Anton'] text-xl">
                          {formatPrice(product.basePrice, product.currency)}
                        </span>
                      )}
                    </div>
                    {(product.reviewCount > 0 || product.soldCount > 0) && (
                      <div className="flex items-center gap-1.5 mt-1.5 text-black/40">
                        {product.reviewCount > 0 && (
                          <span className="inline-flex items-center gap-1">
                            <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" className="block text-amber-500">
                              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                            </svg>
                            <span className="text-[13px] font-bold leading-none text-black/80">{(product.averageRating ?? 0).toFixed(1)}</span>
                            <span className="text-[11px] leading-none">({product.reviewCount})</span>
                          </span>
                        )}
                        {product.reviewCount > 0 && product.soldCount > 0 && <span className="text-black/20">·</span>}
                        {product.soldCount > 0 && (
                          <span className="text-[11px] leading-none">{product.soldCount} sold</span>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}

          {!loading && !error && products.length > 0 && (
            <div className="mt-12 flex flex-col items-center gap-3">
              <p className="text-[11px] font-bold tracking-[0.15em] uppercase text-black/40">
                Showing {products.length} of {totalElements}
              </p>
              {hasNext && (
                <button
                  type="button"
                  disabled={loadingMore}
                  onClick={() => setPage((p) => p + 1)}
                  className="text-[12px] font-bold tracking-[0.15em] uppercase border-2 border-black px-10 py-3.5 hover:bg-black hover:text-white transition-colors disabled:opacity-40 disabled:cursor-wait"
                >
                  {loadingMore ? 'Loading…' : 'Load more'}
                </button>
              )}
            </div>
          )}
        </main>
      </div>

      <FooterFull />
    </div>
  );
}

function SkeletonGrid({ count }) {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="bg-white">
          <div className="bg-black/5 animate-pulse" style={{ paddingTop: '125%' }} />
          <div className="p-4 space-y-2">
            <div className="h-3 w-1/3 bg-black/10 animate-pulse" />
            <div className="h-4 w-2/3 bg-black/10 animate-pulse" />
            <div className="h-6 w-1/4 bg-black/10 animate-pulse" />
          </div>
        </div>
      ))}
    </div>
  );
}

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
}
