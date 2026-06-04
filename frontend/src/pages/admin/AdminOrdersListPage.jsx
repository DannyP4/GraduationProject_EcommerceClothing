import { useEffect, useState } from 'react';
import { Link, useLocation, useSearchParams } from 'react-router-dom';
import OrderStatusBadge from '../../components/admin/OrderStatusBadge';
import * as adminOrderService from '../../services/adminOrderService';
import useScrollRestore from '../../lib/useScrollRestore';
import AdminPagination from '../../components/admin/AdminPagination';

const STATUS_OPTIONS = ['ALL', 'PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'];
const PAGE_SIZE = 20;

export default function AdminOrdersListPage() {
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [statusFilter, setStatusFilter] = useState(() => searchParams.get('status') ?? 'ALL');
  const [search, setSearch] = useState(() => searchParams.get('q') ?? '');
  const [searchTerm, setSearchTerm] = useState(() => searchParams.get('q') ?? '');
  const [page, setPage] = useState(() => Math.max(0, Math.floor(Number(searchParams.get('page')) || 1) - 1));

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadNonce, setReloadNonce] = useState(0);

  useScrollRestore(!loading);

  useEffect(() => {
    const t = setTimeout(() => setSearchTerm(search.trim()), 300);
    return () => clearTimeout(t);
  }, [search]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    const params = {
      page,
      size: PAGE_SIZE,
      ...(statusFilter !== 'ALL' && { status: statusFilter }),
      ...(searchTerm && { search: searchTerm }),
    };

    adminOrderService
      .listOrders(params)
      .then((res) => { if (!cancelled) setData(res); })
      .catch((err) => { if (!cancelled) setError(err.message || 'Could not load orders.'); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [statusFilter, searchTerm, page, reloadNonce]);

  useEffect(() => {
    const next = new URLSearchParams();
    if (statusFilter !== 'ALL') next.set('status', statusFilter);
    if (searchTerm) next.set('q', searchTerm);
    if (page > 0) next.set('page', String(page + 1));
    if (next.toString() !== searchParams.toString()) {
      setSearchParams(next, { replace: true });
    }
  }, [statusFilter, searchTerm, page, searchParams, setSearchParams]);

  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;
  const rows = data?.content ?? [];

  const filtersDirty = !!(search || statusFilter !== 'ALL');
  const clearFilters = () => {
    setSearch('');
    setSearchTerm('');
    setStatusFilter('ALL');
    setPage(0);
  };
  const backTo = `/admin/orders${location.search}`;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Orders</h1>
        <p className="text-sm text-black/55 mt-1 max-w-md">Manage and transition all customer orders.</p>
      </div>

      <div className="bg-white border border-black/10 p-3">
        <div className="flex flex-wrap items-stretch gap-2">
          <input
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            placeholder="Search order # or customer..."
            className="flex-1 min-w-[180px] border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
          />
          <select
            value={statusFilter}
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            className="border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white min-w-[130px]"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s === 'ALL' ? 'All statuses' : s}</option>
            ))}
          </select>
          <button
            type="button"
            onClick={clearFilters}
            disabled={!filtersDirty}
            className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-4 py-2 hover:border-black hover:bg-black hover:text-white transition-colors disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-black/40"
          >
            Clear
          </button>
          <button
            type="button"
            onClick={() => setReloadNonce((n) => n + 1)}
            disabled={loading}
            title="Reload orders"
            className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-4 py-2 hover:border-black hover:bg-black hover:text-white transition-colors disabled:opacity-40 inline-flex items-center gap-1.5"
          >
            <RefreshIcon spinning={loading} /> {loading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && <Banner>{error}</Banner>}

      <div className="bg-white border border-black/10 overflow-hidden">
        <div className="hidden md:grid grid-cols-[1.4fr_1.4fr_0.9fr_0.9fr_1fr_0.6fr] gap-3 px-4 py-3 text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 border-b border-black/10 bg-black/[0.02]">
          <span>Order #</span>
          <span>Customer</span>
          <span>Status</span>
          <span>Payment</span>
          <span>Placed</span>
          <span className="text-right">Total</span>
        </div>

        {loading && !data ? (
          <div className="px-6 py-10 text-center text-sm text-black/40">Loading...</div>
        ) : rows.length === 0 ? (
          <div className="px-6 py-16 text-center text-sm text-black/50">
            No orders match the current filters.
          </div>
        ) : (
          <ul className="divide-y divide-black/5">
            {rows.map((o) => (
              <li key={o.id}>
                <Link
                  to={`/admin/orders/${o.orderNumber}`}
                  state={{ backTo }}
                  className="grid grid-cols-2 md:grid-cols-[1.4fr_1.4fr_0.9fr_0.9fr_1fr_0.6fr] gap-3 px-4 py-4 hover:bg-black/[0.03] transition-colors items-center"
                >
                  <div className="min-w-0">
                    <p className="font-bold text-sm tracking-wider truncate">{o.orderNumber}</p>
                    <p className="md:hidden text-[11px] text-black/50 truncate">{o.customer?.fullName ?? '-'}</p>
                  </div>
                  <div className="hidden md:block min-w-0">
                    <p className="text-sm text-black/80 truncate">{o.customer?.fullName ?? '-'}</p>
                    <p className="text-[11px] text-black/40 truncate">{o.customer?.email}</p>
                  </div>
                  <div><OrderStatusBadge status={o.status} /></div>
                  <div className="hidden md:flex items-center gap-2">
                    <span className="text-[10px] font-bold tracking-wider uppercase text-black/50">
                      {o.paymentProvider ?? '-'}
                    </span>
                    {o.paymentStatus && <OrderStatusBadge status={o.paymentStatus} />}
                  </div>
                  <div className="hidden md:block text-[11px] text-black/50">{formatDate(o.placedAt)}</div>
                  <div className="text-right">
                    <p className="font-['Anton'] text-lg">{formatPrice(o.grandTotal, o.currency)}</p>
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>

      <AdminPagination
        page={page}
        totalPages={totalPages}
        totalElements={totalElements}
        onChange={setPage}
      />
    </div>
  );
}

function RefreshIcon({ spinning }) {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={spinning ? 'animate-spin' : ''}>
      <polyline points="23 4 23 10 17 10" />
      <polyline points="1 20 1 14 7 14" />
      <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
    </svg>
  );
}

function Banner({ children }) {
  return <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs">{children}</div>;
}

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
}

function formatDate(iso) {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: '2-digit',
      hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}
