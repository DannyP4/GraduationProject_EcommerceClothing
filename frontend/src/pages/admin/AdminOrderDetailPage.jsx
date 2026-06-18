import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import OrderStatusBadge from '../../components/admin/OrderStatusBadge';
import ConfirmDialog from '../../components/ConfirmDialog';
import { goBack } from '../../lib/historyBack';
import * as adminOrderService from '../../services/adminOrderService';

const GHN_TRACKING_URL = 'https://tracking.ghn.dev/?order_code=';

export default function AdminOrderDetailPage() {
  const { orderNumber } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const backTo = location.state?.backTo || '/admin/orders';

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [confirmTransition, setConfirmTransition] = useState(null);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [confirmRefund, setConfirmRefund] = useState(false);
  const [actionMsg, setActionMsg] = useState(null);
  const [acting, setActing] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const d = await adminOrderService.getOrder(orderNumber);
      setData(d);
    } catch (e) {
      setError(e.message || 'Could not load order.');
    } finally {
      setLoading(false);
    }
  }, [orderNumber]);

  useEffect(() => { refresh(); }, [refresh]);

  const doTransition = async (target) => {
    setConfirmTransition(null);
    setActing(true);
    setActionMsg(null);
    try {
      const updated = await adminOrderService.transitionOrder(orderNumber, { targetStatus: target });
      setData(updated);
      setActionMsg({ type: 'success', text: `Status updated to ${target}.` });
    } catch (e) {
      setActionMsg({ type: 'error', text: e.message || 'Transition failed.' });
    } finally {
      setActing(false);
    }
  };

  const doCancel = async () => {
    setConfirmCancel(false);
    setActing(true);
    setActionMsg(null);
    try {
      const updated = await adminOrderService.cancelOrder(orderNumber, { reason: 'Cancelled by admin' });
      setData(updated);
      setActionMsg({
        type: updated.requiresRefund ? 'warning' : 'success',
        text: updated.requiresRefund
          ? 'Cancelled. Captured payment still needs a refund — use Refund Payment below.'
          : 'Cancelled and stock restored.',
      });
    } catch (e) {
      setActionMsg({ type: 'error', text: e.message || 'Cancel failed.' });
    } finally {
      setActing(false);
    }
  };

  const doRefund = async () => {
    setConfirmRefund(false);
    setActing(true);
    setActionMsg(null);
    try {
      const updated = await adminOrderService.refundOrder(orderNumber, { reason: 'Refunded by admin' });
      setData(updated);
      const provider = updated.order?.payment?.provider;
      setActionMsg({
        type: 'success',
        text: provider === 'STRIPE'
          ? 'Refunded via Stripe at the original FX rate.'
          : provider === 'VNPAY'
            ? 'Refunded via VNPAY.'
            : 'Refunded (offline payment marked refunded).',
      });
    } catch (e) {
      setActionMsg({ type: 'error', text: e.message || 'Refund failed.' });
    } finally {
      setActing(false);
    }
  };

  if (loading && !data) return <p className="text-sm text-black/40">Loading...</p>;
  if (error) {
    return (
      <div className="bg-white border border-black/10 p-8 text-center">
        <Banner type="error">{error}</Banner>
        <Link
          to={backTo}
          className="inline-block mt-4 text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
        >
          Back to Orders
        </Link>
      </div>
    );
  }
  if (!data) return null;

  const order = data.order;
  const customer = data.customer;
  const allowed = data.allowedTransitions ?? [];
  const cancellable = data.cancellableByAdmin;
  const refundable = data.refundableByAdmin;
  const requiresRefund = data.requiresRefund;

  return (
    <div className="space-y-6">
      <div>
        <button
          type="button"
          onClick={() => goBack(navigate, location, '/admin/orders')}
          className="inline-flex items-center gap-2 text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-3 py-2 hover:bg-black hover:text-white transition-colors"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          All Orders
        </button>
      </div>

      <div>
        <div className="flex items-center gap-3 flex-wrap">
          <h1 className="font-['Anton'] text-4xl uppercase tracking-tight">{order.orderNumber}</h1>
          <OrderStatusBadge status={order.status} size="md" />
        </div>
        <p className="text-xs text-black/50 mt-1">Placed {formatDate(order.placedAt)}</p>
      </div>

      {actionMsg && <Banner type={actionMsg.type}>{actionMsg.text}</Banner>}
      {requiresRefund && (
        <Banner type="warning">
          Order was cancelled while payment was already captured. Use Refund Payment to return the money.
        </Banner>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-6 items-start">
        <div className="space-y-5">
          <Section title="Items">
            <ul className="divide-y divide-black/8">
              {order.items.map((it) => (
                <ItemRow key={it.id} item={it} currency={order.currency} />
              ))}
            </ul>
          </Section>

          <Section title="Customer">
            <p className="text-sm font-bold">{customer?.fullName ?? '-'}</p>
            <p className="text-xs text-black/60">{customer?.email}</p>
            {customer?.phone && <p className="text-xs text-black/60">{customer.phone}</p>}
            <p className="text-[11px] text-black/40 mt-1">Customer ID #{customer?.id}</p>
          </Section>

          <Section title="Shipping Address">
            <p className="font-bold text-sm">{order.shippingRecipient}</p>
            <p className="text-xs text-black/60 mb-1">{order.shippingPhone}</p>
            <p className="text-xs text-black/70 leading-relaxed">
              {order.shippingLine1}
              {order.shippingWard ? `, ${order.shippingWard}` : ''}, {order.shippingDistrict}, {order.shippingCity}
              {order.shippingPostalCode ? ` ${order.shippingPostalCode}` : ''} · {order.shippingCountry}
            </p>
            {order.ghnOrderCode && (
              <div className="mt-3 pt-3 border-t border-black/8">
                <p className="text-[10px] font-bold tracking-wider uppercase text-black/40 mb-1">GHN Shipment</p>
                <a
                  href={`${GHN_TRACKING_URL}${order.ghnOrderCode}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-xs font-bold text-[#E83354] hover:underline"
                >
                  {order.ghnOrderCode} ↗
                </a>
              </div>
            )}
            {order.notes && (
              <div className="mt-3 pt-3 border-t border-black/8">
                <p className="text-[10px] font-bold tracking-wider uppercase text-black/40 mb-1">Customer Notes</p>
                <p className="text-xs text-black/70 whitespace-pre-line">{order.notes}</p>
              </div>
            )}
          </Section>

          {order.payment && (
            <Section title="Payment">
              <div className="grid grid-cols-2 gap-3 text-xs">
                <Field label="Provider" value={order.payment.provider} />
                <Field label="Status" value={<OrderStatusBadge status={order.payment.status} />} />
                <Field label="Amount" value={formatMoney(order.payment.amount, order.payment.currency)} />
                <Field label="Paid at" value={order.payment.paidAt ? formatDate(order.payment.paidAt) : '-'} />
              </div>
            </Section>
          )}

          <Section title="Status Timeline">
            <ul className="space-y-3">
              {order.statusHistory.map((h) => (
                <li key={h.id} className="flex gap-3 items-start">
                  <span className="w-2 h-2 rounded-full bg-[#E83354] mt-1.5 flex-shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <OrderStatusBadge status={h.status} />
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
              <Row label="Subtotal" value={formatMoney(order.subtotal, order.currency)} />
              <Row label="Discount" value={`- ${formatMoney(order.discountTotal, order.currency)}`} />
              <Row label="Shipping" value={formatMoney(order.shippingCost, order.currency)} />
              <Row label="Tax" value={formatMoney(order.taxTotal, order.currency)} />
            </div>
            <div className="border-t border-white/15 pt-3 flex justify-between items-baseline">
              <span className="text-[11px] font-bold tracking-[0.1em] uppercase text-white/60">Total</span>
              <span className="font-['Anton'] text-2xl">{formatMoney(order.grandTotal, order.currency)}</span>
            </div>
          </div>

          <div className="bg-white border border-black/10 p-5 space-y-3">
            <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40">Admin Actions</p>

            {allowed.length === 0 && !cancellable && !refundable && (
              <p className="text-[11px] text-black/40 tracking-wider">No further actions available.</p>
            )}

            {allowed.map((target) => (
              <button
                key={target}
                disabled={acting}
                onClick={() => setConfirmTransition(target)}
                className="w-full text-[11px] font-bold tracking-[0.15em] uppercase py-3 bg-black text-white hover:bg-[#E83354] disabled:opacity-50 transition-colors"
              >
                Transition to {target}
              </button>
            ))}

            {cancellable && (
              <button
                disabled={acting}
                onClick={() => setConfirmCancel(true)}
                className="w-full text-[11px] font-bold tracking-[0.15em] uppercase py-3 border border-[#E83354] text-[#E83354] hover:bg-[#E83354] hover:text-white disabled:opacity-50 transition-colors"
              >
                Cancel Order
              </button>
            )}

            {refundable && (
              <button
                disabled={acting}
                onClick={() => setConfirmRefund(true)}
                className="w-full text-[11px] font-bold tracking-[0.15em] uppercase py-3 border border-black text-black hover:bg-black hover:text-white disabled:opacity-50 transition-colors"
              >
                Refund Payment
              </button>
            )}
          </div>
        </aside>
      </div>

      <ConfirmDialog
        open={!!confirmTransition}
        title={`Move order to ${confirmTransition}?`}
        message={`This will update the order status to ${confirmTransition} and append an entry to the status timeline with your email.`}
        confirmLabel={`Move to ${confirmTransition}`}
        cancelLabel="Cancel"
        onCancel={() => setConfirmTransition(null)}
        onConfirm={() => doTransition(confirmTransition)}
      />

      <ConfirmDialog
        open={confirmCancel}
        title="Cancel this order?"
        message={`Order ${order.orderNumber} will be cancelled and any reserved stock returned. Captured payments can then be refunded via Refund Payment.`}
        confirmLabel="Cancel Order"
        cancelLabel="Keep Order"
        tone="danger"
        onCancel={() => setConfirmCancel(false)}
        onConfirm={doCancel}
      />

      <ConfirmDialog
        open={confirmRefund}
        title="Refund this payment?"
        message={refundDialogMessage(order)}
        confirmLabel="Refund Payment"
        cancelLabel="Keep As-Is"
        tone="danger"
        onCancel={() => setConfirmRefund(false)}
        onConfirm={doRefund}
      />
    </div>
  );
}

function refundDialogMessage(order) {
  const p = order.payment;
  const amount = p ? formatMoney(p.amount, p.currency) : formatMoney(order.grandTotal, order.currency);
  const gateway = p?.provider === 'STRIPE'
    ? 'Stripe will be refunded at the original FX rate'
    : p?.provider === 'VNPAY'
      ? 'VNPAY will be refunded'
      : 'The offline payment will be marked refunded';
  const outcome = order.status === 'CANCELLED'
    ? 'The order stays CANCELLED.'
    : 'The order will move to REFUNDED.';
  const stock = order.status === 'PAID' || order.status === 'PROCESSING'
    ? ' Reserved stock will be returned.'
    : '';
  return `${gateway} (${amount}). ${outcome}${stock}`;
}

function Section({ title, children }) {
  return (
    <section className="border border-black/10 bg-white p-5">
      <h3 className="font-['Anton'] text-lg uppercase tracking-wider mb-4">{title}</h3>
      {children}
    </section>
  );
}

function ItemRow({ item, currency }) {
  return (
    <li className="flex gap-3 py-3">
      <div className="w-16 h-20 flex-shrink-0 bg-black/5 overflow-hidden">
        {item.imageUrl && <img src={item.imageUrl} alt={item.productName} className="w-full h-full object-cover" />}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-bold uppercase tracking-wider truncate">{item.productName}</p>
        <p className="text-[11px] text-black/50">{item.variantLabel} · SKU {item.sku}</p>
        <p className="text-[11px] text-black/50">Qty {item.quantity} × {formatMoney(item.unitPrice, currency)}</p>
      </div>
      <span className="text-sm font-bold whitespace-nowrap self-center">
        {formatMoney(item.lineTotal, currency)}
      </span>
    </li>
  );
}

function Field({ label, value }) {
  return (
    <div>
      <p className="text-[10px] font-bold tracking-wider uppercase text-black/40 mb-1">{label}</p>
      <div className="text-xs text-black/80 truncate">{value}</div>
    </div>
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
    : type === 'warning'
      ? 'border-amber-400/40 bg-amber-50 text-amber-800'
      : 'border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354]';
  return <div className={`border px-4 py-3 text-xs ${cls}`}>{children}</div>;
}

function formatMoney(value, currency) {
  if (value == null) return '-';
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
