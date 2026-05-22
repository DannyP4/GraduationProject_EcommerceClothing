import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import OrderStatusBadge from '../../components/admin/OrderStatusBadge';
import * as adminOrderService from '../../services/adminOrderService';

const STATUS_OPTIONS = ['ALL', 'PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'];
const PAGE_SIZE = 10;

export default function AdminOrdersListPage() {
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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
  }, [statusFilter, searchTerm, page]);

  const totalPages = data?.totalPages ?? 1;
  const totalElements = data?.totalElements ?? 0;
  const rows = data?.content ?? [];

  const resetPage = () => setPage(0);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Orders</h1>
        <p className="text-xs text-black/50 mt-1">Manage and transition all customer orders.</p>
      </div>

      <div className="bg-white border border-black/10 p-4 flex flex-col md:flex-row md:items-center gap-3">
        <div className="flex flex-wrap gap-1">
          {STATUS_OPTIONS.map((s) => (
            <button
              key={s}
              onClick={() => { setStatusFilter(s); resetPage(); }}
              className={`text-[10px] font-bold tracking-[0.15em] uppercase px-3 py-2 border transition-colors ${
                statusFilter === s
                  ? 'bg-black text-white border-black'
                  : 'bg-white text-black/60 border-black/10 hover:border-black hover:text-black'
              }`}
            >
              {s}
            </button>
          ))}
        </div>

        <div className="md:ml-auto flex items-center gap-2 flex-1 md:flex-initial md:w-72 border border-black/15 px-3 py-2 focus-within:border-black">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-black/40">
            <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
          </svg>
          <input
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); resetPage(); }}
            placeholder="Order # or customer..."
            className="flex-1 bg-transparent text-sm focus:outline-none placeholder:text-black/30"
          />
          {search && (
            <button onClick={() => { setSearch(''); resetPage(); }} className="text-black/30 hover:text-black" aria-label="Clear search">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )}
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

      {totalElements > 0 && (
        <div className="flex items-center justify-between text-[11px] tracking-wider">
          <span className="text-black/40">
            Showing {page * PAGE_SIZE + 1}-{Math.min(totalElements, (page + 1) * PAGE_SIZE)} of {totalElements}
          </span>
          <div className="flex gap-2">
            <button
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-4 py-2 hover:border-black disabled:opacity-30 disabled:cursor-not-allowed"
            >
              Prev
            </button>
            <span className="text-[11px] text-black/50 self-center px-2">
              Page {page + 1} / {totalPages}
            </span>
            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage(page + 1)}
              className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-4 py-2 hover:border-black disabled:opacity-30 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
        </div>
      )}
    </div>
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
