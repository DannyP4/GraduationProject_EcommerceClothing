import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as orderService from '../services/orderService';

const PAGE_SIZE = 10;

export default function AccountOrdersPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    orderService.listOrders({ page, size: PAGE_SIZE })
      .then((res) => { if (!cancelled) setData(res); })
      .catch((err) => { if (!cancelled) setError(err.message || 'Could not load orders.'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [page]);

  return (
    <div>
      <div className="mb-6">
        <h2 className="font-['Anton'] text-3xl uppercase tracking-tight">Order History</h2>
        <p className="text-xs text-black/50 mt-1">All orders placed under your account.</p>
      </div>

      {error && <Banner>{error}</Banner>}

      {loading && !data ? (
        <p className="text-sm text-black/40">Loading…</p>
      ) : !data || data.content.length === 0 ? (
        <EmptyOrders />
      ) : (
        <>
          <ul className="space-y-3">
            {data.content.map((o) => (
              <OrderRow key={o.id} order={o} />
            ))}
          </ul>

          {data.totalPages > 1 && (
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              hasPrev={data.hasPrevious}
              hasNext={data.hasNext}
              onPage={setPage}
            />
          )}
        </>
      )}
    </div>
  );
}

function OrderRow({ order }) {
  return (
    <li>
      <Link
        to={`/account/orders/${order.orderNumber}`}
        className="group flex gap-4 border border-black/10 bg-white p-4 transition-all duration-300 ease-out hover:border-black hover:-translate-y-0.5 hover:shadow-[0_10px_20px_-12px_rgba(0,0,0,0.25)]"
      >
        <div className="w-16 h-20 bg-black/5 overflow-hidden flex-shrink-0">
          {order.thumbnailUrl && (
            <img src={order.thumbnailUrl} alt="" className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105" />
          )}
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <span className="font-bold text-sm tracking-wider group-hover:text-[#E83354] transition-colors">{order.orderNumber}</span>
            <StatusBadge status={order.status} />
          </div>
          <p className="text-xs text-black/60 truncate">
            {order.firstItemName ?? '—'}
            {order.itemCount > 1 ? ` + ${order.itemCount - 1} more` : ''}
          </p>
          <p className="text-[11px] text-black/40 mt-1">{formatDate(order.placedAt)}</p>
        </div>

        <div className="text-right whitespace-nowrap flex flex-col items-end">
          <p className="font-['Anton'] text-xl">{formatPrice(order.grandTotal, order.currency)}</p>
          <span className="mt-2 inline-flex items-center gap-1 text-[11px] font-bold tracking-[0.15em] uppercase text-black/40 group-hover:text-[#E83354] transition-colors">
            View Detail
            <span className="inline-block transition-transform duration-200 group-hover:translate-x-1">→</span>
          </span>
        </div>
      </Link>
    </li>
  );
}

function StatusBadge({ status }) {
  const styles = {
    PENDING: 'bg-amber-50    text-amber-700  border-amber-300',
    PAID: 'bg-blue-50     text-blue-700   border-blue-300',
    PROCESSING: 'bg-blue-50     text-blue-700   border-blue-300',
    SHIPPED: 'bg-indigo-50   text-indigo-700 border-indigo-300',
    DELIVERED: 'bg-green-50    text-green-700  border-green-400',
    CANCELLED: 'bg-[#E83354]/10 text-[#E83354] border-[#E83354]/50',
    REFUNDED: 'bg-purple-50   text-purple-700 border-purple-300',
  };
  const cls = styles[status] ?? 'bg-black/5 text-black/60 border-black/15';
  return (
    <span className={`text-[10px] font-bold tracking-wider uppercase px-2 py-0.5 border ${cls}`}>
      {status}
    </span>
  );
}

function Pagination({ page, totalPages, hasPrev, hasNext, onPage }) {
  return (
    <div className="flex items-center justify-between mt-6">
      <button
        disabled={!hasPrev}
        onClick={() => onPage(page - 1)}
        className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-4 py-2 hover:border-black disabled:opacity-30 disabled:cursor-not-allowed"
      >
        ← Prev
      </button>
      <span className="text-[11px] tracking-wider text-black/50">
        Page {page + 1} of {totalPages}
      </span>
      <button
        disabled={!hasNext}
        onClick={() => onPage(page + 1)}
        className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-4 py-2 hover:border-black disabled:opacity-30 disabled:cursor-not-allowed"
      >
        Next →
      </button>
    </div>
  );
}

function EmptyOrders() {
  return (
    <div className="border border-dashed border-black/15 px-6 py-16 text-center">
      <p className="text-sm text-black/50 mb-4">You haven't placed any orders yet.</p>
      <Link
        to="/shop"
        className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
      >
        Start Shopping
      </Link>
    </div>
  );
}

function Banner({ children }) {
  return <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs mb-4">{children}</div>;
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
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}
