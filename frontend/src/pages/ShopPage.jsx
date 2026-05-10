import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import { getProducts, getCategories } from '../services/productService';

const SORT_OPTIONS = [
  { label: 'Newest', value: 'NEWEST' },
  { label: 'Price: Low to High', value: 'PRICE_ASC' },
  { label: 'Price: High to Low', value: 'PRICE_DESC' },
  { label: 'Popular', value: 'POPULAR' },
];

const PAGE_SIZE = 12;

export default function ShopPage() {
  const navigate = useNavigate();

  const [categories, setCategories] = useState([]);
  const [activeCategoryId, setActiveCategoryId] = useState(null);
  const [sort, setSort] = useState('NEWEST');
  const [page, setPage] = useState(0);

  const [pageData, setPageData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => { setPage(0); }, [activeCategoryId, sort]);

  useEffect(() => {
    let cancelled = false;
    getCategories()
      .then((data) => { if (!cancelled) setCategories(data || []); })
      .catch(() => { });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getProducts({
      page,
      size: PAGE_SIZE,
      sort,
      categoryId: activeCategoryId ?? undefined,
    })
      .then((data) => { if (!cancelled) setPageData(data); })
      .catch((err) => { if (!cancelled) setError(err.message || 'Failed to load products'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [page, sort, activeCategoryId]);

  const products = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const totalElements = pageData?.totalElements ?? 0;

  const pageNumbers = useMemo(() => buildPageList(page, totalPages), [page, totalPages]);

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />

      {/* Page header */}
      <div className="bg-[#0A0A0A] text-white py-12 px-6">
        <div className="max-w-[1440px] mx-auto">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-white/40 mb-2">
            SS 2026 · {loading ? '…' : `${totalElements} items`}
          </p>
          <h1 className="font-['Anton'] text-5xl md:text-7xl tracking-tight uppercase">
            Shop All
          </h1>
        </div>
      </div>

      <div className="max-w-[1440px] mx-auto px-6 py-10 flex gap-8">
        {/* SIDEBAR */}
        <aside className="w-56 flex-shrink-0">
          <div className="sticky top-20 space-y-8">
            {/* Categories */}
            <div>
              <h3 className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40 mb-4">Collections</h3>
              <ul className="space-y-1">
                <li>
                  <button
                    onClick={() => setActiveCategoryId(null)}
                    className={`w-full text-left text-sm py-1.5 px-2 transition-all font-medium ${activeCategoryId === null
                      ? 'bg-black text-white'
                      : 'text-black/60 hover:text-black hover:bg-black/5'
                      }`}
                  >
                    All
                  </button>
                </li>
                {categories.map((c) => (
                  <li key={c.id}>
                    <button
                      onClick={() => setActiveCategoryId(c.id)}
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
                onChange={(e) => setSort(e.target.value)}
                className="w-full border border-black/15 bg-white text-sm px-3 py-2.5 focus:outline-none focus:border-black appearance-none cursor-pointer"
              >
                {SORT_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </div>

            {activeCategoryId !== null && (
              <button
                onClick={() => setActiveCategoryId(null)}
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
                onClick={() => setPage((p) => p)}
                className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
              >
                Retry
              </button>
            </div>
          ) : loading ? (
            <SkeletonGrid count={PAGE_SIZE} />
          ) : products.length === 0 ? (
            <div className="text-center py-24 text-black/40">
              <p className="text-xl font-bold mb-2">No products found</p>
              <p className="text-sm">Try adjusting your filters</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
              {products.map((product) => (
                <div
                  key={product.id}
                  className="group bg-white cursor-pointer relative overflow-hidden transition-all duration-300 ease-out hover:-translate-y-1 hover:shadow-[0_12px_24px_-12px_rgba(0,0,0,0.25)]"
                  onClick={() => navigate(`/product/${product.slug || product.id}`)}
                >
                  <div className="relative overflow-hidden" style={{ paddingTop: '125%' }}>
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
                    <h3 className="text-sm font-bold uppercase tracking-wider mb-2 group-hover:text-[#E83354] transition-colors">{product.name}</h3>
                    <div className="flex items-center gap-2">
                      <span className="font-['Anton'] text-xl">
                        {formatPrice(product.basePrice, product.currency)}
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Pagination */}
          {!loading && !error && totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-12">
              <button
                disabled={!pageData?.hasPrevious}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 h-9 text-sm font-bold transition-all bg-white text-black/60 border border-black/15 hover:border-black hover:text-black disabled:opacity-30 disabled:cursor-not-allowed"
              >
                Prev
              </button>
              {pageNumbers.map((p, i) =>
                p === '…' ? (
                  <span key={`gap-${i}`} className="w-9 h-9 flex items-center justify-center text-black/40">…</span>
                ) : (
                  <button
                    key={p}
                    onClick={() => setPage(p - 1)}
                    className={`w-9 h-9 text-sm font-bold transition-all ${p - 1 === page
                      ? 'bg-black text-white'
                      : 'bg-white text-black/60 border border-black/15 hover:border-black hover:text-black'
                      }`}
                  >
                    {p}
                  </button>
                )
              )}
              <button
                disabled={!pageData?.hasNext}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 h-9 text-sm font-bold transition-all bg-white text-black/60 border border-black/15 hover:border-black hover:text-black disabled:opacity-30 disabled:cursor-not-allowed"
              >
                Next
              </button>
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

function buildPageList(currentPage, totalPages) {
  if (totalPages <= 1) return [];
  const cur = currentPage + 1;
  const last = totalPages;
  const range = [];
  range.push(1);
  if (cur - 2 > 2) range.push('…');
  for (let i = Math.max(2, cur - 1); i <= Math.min(last - 1, cur + 1); i++) {
    range.push(i);
  }
  if (cur + 2 < last - 1) range.push('…');
  if (last > 1) range.push(last);
  return range;
}

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
}
