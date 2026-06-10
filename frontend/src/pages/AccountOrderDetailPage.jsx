import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import * as orderService from '../services/orderService';
import * as paymentService from '../services/paymentService';
import { getSimilarToProducts } from '../services/productService';
import { Carousel } from '../components/RecommendationRow';
import ConfirmDialog from '../components/ConfirmDialog';

export default function AccountOrderDetailPage() {
  const { orderNumber } = useParams();

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [confirmCancel, setConfirmCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [retrying, setRetrying] = useState(false);
  const [actionMsg, setActionMsg] = useState(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const o = await orderService.getOrder(orderNumber);
      setOrder(o);
    } catch (err) {
      setError(err.message || 'Could not load order.');
    } finally {
      setLoading(false);
    }
  }, [orderNumber]);

  useEffect(() => { refresh(); }, [refresh]);

  const [recs, setRecs] = useState([]);
  const orderIdsKey = [...new Set((order?.items ?? []).map((i) => i.productId).filter(Boolean))]
    .sort((a, b) => a - b)
    .join(',');

  useEffect(() => {
    if (!orderIdsKey) { setRecs([]); return undefined; }
    let cancelled = false;
    getSimilarToProducts(orderIdsKey.split(',').map(Number), 12)
      .then((d) => { if (!cancelled) setRecs(d || []); })
      .catch(() => { if (!cancelled) setRecs([]); });
    return () => { cancelled = true; };
  }, [orderIdsKey]);

  const doCancel = async () => {
    setConfirmCancel(false);
    setCancelling(true);
    setActionMsg(null);
    try {
      const updated = await orderService.cancelOrder(orderNumber);
      setOrder(updated);
      setActionMsg({ type: 'success', text: 'Order cancelled. Stock has been released.' });
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Could not cancel order.' });
    } finally {
      setCancelling(false);
    }
  };

  const doContinuePay = async () => {
    setRetrying(true);
    setActionMsg(null);
    try {
      const result = await paymentService.retryPayment(orderNumber);
      if (result?.redirectUrl) {
        window.location.assign(result.redirectUrl);
        return;
      }
      setActionMsg({ type: 'error', text: 'Could not obtain a payment URL. Try again.' });
    } catch (err) {
      setActionMsg({ type: 'error', text: err.message || 'Could not initiate payment.' });
    } finally {
      setRetrying(false);
    }
  };

  const canContinuePay =
    order?.status === 'PENDING'
    && order?.payment
    && order.payment.provider !== 'COD'
    && order.payment.status !== 'CAPTURED';

  if (loading && !order) return <p className="text-sm text-black/40">Loading…</p>;
  if (error) {
    return (
      <div>
        <Banner type="error">{error}</Banner>
        <Link to="/account/orders" className="text-[11px] font-bold tracking-[0.15em] uppercase underline">
          ← Back to Orders
        </Link>
      </div>
    );
  }
  if (!order) return null;

  return (
    <div>
      <div className="mb-6">
        <Link to="/account/orders" className="text-[11px] font-bold tracking-[0.15em] uppercase text-black/50 hover:text-black">
          ← Order History
        </Link>
        <div className="flex items-center gap-3 mt-2 flex-wrap">
          <h2 className="font-['Anton'] text-3xl uppercase tracking-tight">{order.orderNumber}</h2>
          <StatusBadge status={order.status} />
        </div>
        <p className="text-xs text-black/50 mt-1">Placed {formatDate(order.placedAt)}</p>
      </div>

      {actionMsg && <Banner type={actionMsg.type}>{actionMsg.text}</Banner>}

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-6 items-start">
        <div className="space-y-5">
          <Section title="Items">
            <ul className="divide-y divide-black/8">
              {order.items.map((it) => (
                <ItemRow key={it.id} item={it} currency={order.currency} />
              ))}
            </ul>
          </Section>

          <Section title="Shipping Address">
            <p className="font-bold text-sm">{order.shippingRecipient}</p>
            <p className="text-xs text-black/60 mb-1">{order.shippingPhone}</p>
            <p className="text-xs text-black/70 leading-relaxed">
              {order.shippingLine1}
              {order.shippingWard ? `, ${order.shippingWard}` : ''}, {order.shippingDistrict},{' '}
              {order.shippingCity}
              {order.shippingPostalCode ? ` ${order.shippingPostalCode}` : ''} · {order.shippingCountry}
            </p>
            {order.notes && (
              <div className="mt-3 pt-3 border-t border-black/8">
                <p className="text-[10px] font-bold tracking-wider uppercase text-black/40 mb-1">Notes</p>
                <p className="text-xs text-black/70 whitespace-pre-line">{order.notes}</p>
              </div>
            )}
          </Section>

          <Section title="Status Timeline">
            <ul className="space-y-3">
              {order.statusHistory.map((h) => (
                <li key={h.id} className="flex gap-3 items-start">
                  <span className="w-2 h-2 rounded-full bg-[#E83354] mt-1.5 flex-shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <StatusBadge status={h.status} />
                      <span className="text-[10px] text-black/40">{formatDate(h.changedAt)}</span>
                    </div>
                    {h.note && <p className="text-xs text-black/60 mt-1">{h.note}</p>}
                  </div>
                </li>
              ))}
            </ul>
          </Section>
        </div>

        <aside className="space-y-4 lg:sticky lg:top-20">
          <div className="bg-[#0A0A0A] text-white p-5">
            <h3 className="font-['Anton'] text-xl uppercase tracking-wider mb-4">Summary</h3>
            <div className="space-y-2 mb-4 text-sm">
              <Row label="Subtotal" value={formatPrice(order.subtotal, order.currency)} />
              <Row label="Discount" value={`- ${formatPrice(order.discountTotal, order.currency)}`} />
              <Row label="Shipping" value={formatPrice(order.shippingCost, order.currency)} />
              <Row label="Tax" value={formatPrice(order.taxTotal, order.currency)} />
            </div>
            <div className="border-t border-white/15 pt-3">
              <div className="flex justify-between items-baseline">
                <span className="text-[11px] font-bold tracking-[0.1em] uppercase text-white/60">Total</span>
                <span className="font-['Anton'] text-2xl">{formatPrice(order.grandTotal, order.currency)}</span>
              </div>
            </div>

            {order.payment && (
              <div className="mt-4 pt-4 border-t border-white/15">
                <p className="text-[10px] font-bold tracking-wider uppercase text-white/40 mb-1">Payment</p>
                <p className="text-sm">{order.payment.provider} · <span className="text-white/60">{order.payment.status}</span></p>
                {order.payment.paidAt && (
                  <p className="text-[10px] text-white/40 mt-0.5">Paid at {formatDate(order.payment.paidAt)}</p>
                )}
              </div>
            )}
          </div>

          {canContinuePay && (
            <button
              onClick={doContinuePay}
              disabled={retrying}
              className="w-full bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase py-3 hover:bg-[#c82244] transition-colors disabled:opacity-50"
            >
              {retrying ? 'Redirecting…' : `Continue Paying (${order.payment.provider})`}
            </button>
          )}

          {order.cancellable && (
            <button
              onClick={() => setConfirmCancel(true)}
              disabled={cancelling}
              className="w-full border border-black/20 text-black text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:border-[#E83354] hover:text-[#E83354] transition-colors disabled:opacity-50"
            >
              {cancelling ? 'Cancelling…' : 'Cancel Order'}
            </button>
          )}
        </aside>
      </div>

      {recs.length > 0 && <Carousel title="You may also like" items={recs} />}

      <ConfirmDialog
        open={confirmCancel}
        title="Cancel this order?"
        message={`Order ${order.orderNumber} will be cancelled and stock returned. This cannot be undone.`}
        confirmLabel="Cancel Order"
        cancelLabel="Keep Order"
        tone="danger"
        onCancel={() => setConfirmCancel(false)}
        onConfirm={doCancel}
      />
    </div>
  );
}

function Section({ title, children }) {
  return (
    <section className="border border-black/10 p-5">
      <h3 className="font-['Anton'] text-lg uppercase tracking-wider mb-4">{title}</h3>
      {children}
    </section>
  );
}

function ItemRow({ item, currency }) {
  return (
    <li className="flex gap-3 py-3">
      <Link
        to={item.productSlug ? `/product/${item.productSlug}` : '#'}
        className="w-16 h-20 flex-shrink-0 bg-black/5 overflow-hidden"
      >
        {item.imageUrl && (
          <img src={item.imageUrl} alt={item.productName} className="w-full h-full object-cover" />
        )}
      </Link>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-bold uppercase tracking-wider truncate">{item.productName}</p>
        <p className="text-[11px] text-black/50">{item.variantLabel} · SKU {item.sku}</p>
        <p className="text-[11px] text-black/50">Qty {item.quantity} × {formatPrice(item.unitPrice, currency)}</p>
      </div>
      <span className="text-sm font-bold whitespace-nowrap">
        {formatPrice(item.lineTotal, currency)}
      </span>
    </li>
  );
}

function StatusBadge({ status }) {
  const styles = {
    PENDING:    'bg-amber-50    text-amber-700  border-amber-300',
    PAID:       'bg-blue-50     text-blue-700   border-blue-300',
    PROCESSING: 'bg-blue-50     text-blue-700   border-blue-300',
    SHIPPED:    'bg-indigo-50   text-indigo-700 border-indigo-300',
    DELIVERED:  'bg-green-50    text-green-700  border-green-400',
    CANCELLED:  'bg-[#E83354]/10 text-[#E83354] border-[#E83354]/50',
    REFUNDED:   'bg-purple-50   text-purple-700 border-purple-300',
  };
  const cls = styles[status] ?? 'bg-black/5 text-black/60 border-black/15';
  return (
    <span className={`text-[10px] font-bold tracking-wider uppercase px-2 py-0.5 border ${cls}`}>
      {status}
    </span>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between items-baseline">
      <span className="text-white/60">{label}</span>
      <span className="font-bold">{value}</span>
    </div>
  );
}

function Banner({ type, children }) {
  const cls = type === 'success'
    ? 'border-green-600/30 bg-green-600/10 text-green-700'
    : 'border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354]';
  return <div className={`border px-4 py-3 text-xs mb-4 ${cls}`}>{children}</div>;
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
