import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import ConfirmDialog from '../components/ConfirmDialog';
import { useToast } from '../components/Toast';
import { useCart } from '../context/CartContext';
import * as addressService from '../services/addressService';
import * as orderService from '../services/orderService';
import * as couponService from '../services/couponService';

export default function CheckoutPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { items, cartCount, subtotal, currency, isLoading: cartLoading, clearCart } = useCart();

  const [addresses, setAddresses] = useState([]);
  const [addressesLoading, setAddressesLoading] = useState(true);
  const [addressesError, setAddressesError] = useState(null);

  const [selectedAddressId, setSelectedAddressId] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('COD');
  const [notes, setNotes] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const [couponInput, setCouponInput] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState(null);
  const [couponLoading, setCouponLoading] = useState(false);
  const [couponError, setCouponError] = useState(null);

  const shippingCost = 0;
  const taxTotal = 0;
  const discountTotal = appliedCoupon ? Number(appliedCoupon.discountAmount ?? 0) : 0;
  const grandTotal = useMemo(
    () => Math.max(0, Number(subtotal ?? 0) + shippingCost + taxTotal - discountTotal),
    [subtotal, discountTotal]
  );

  const applyCoupon = async () => {
    const code = couponInput.trim();
    if (!code) return;
    setCouponLoading(true);
    setCouponError(null);
    try {
      const result = await couponService.validateCoupon(code);
      setAppliedCoupon(result);
      setCouponInput('');
    } catch (err) {
      setAppliedCoupon(null);
      setCouponError(err.message || 'Invalid coupon code.');
    } finally {
      setCouponLoading(false);
    }
  };

  const removeCoupon = () => {
    setAppliedCoupon(null);
    setCouponError(null);
  };

  useEffect(() => {
    let cancelled = false;
    setAddressesLoading(true);
    addressService.listAddresses()
      .then((list) => {
        if (cancelled) return;
        const safe = list ?? [];
        setAddresses(safe);
        const def = safe.find((a) => a.isDefault) ?? safe[0];
        if (def) setSelectedAddressId(def.id);
      })
      .catch((err) => {
        if (!cancelled) setAddressesError(err.message || 'Could not load addresses.');
      })
      .finally(() => { if (!cancelled) setAddressesLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const blockedItems = items.filter(
    (i) => i.stockStatus === 'OUT_OF_STOCK' || i.stockStatus === 'UNAVAILABLE'
  );
  const cartReady = items.length > 0 && blockedItems.length === 0;
  const validMethod = ['COD', 'VNPAY', 'STRIPE'].includes(paymentMethod);
  const canPlaceOrder = !submitting && cartReady && !!selectedAddressId && validMethod;

  const askConfirm = () => {
    if (!canPlaceOrder) return;
    setSubmitError(null);
    setConfirmOpen(true);
  };

  const doPlaceOrder = async () => {
    setConfirmOpen(false);
    setSubmitting(true);
    setSubmitError(null);
    try {
      const result = await orderService.placeOrder({
        addressId: selectedAddressId,
        paymentMethod,
        notes: notes.trim() || undefined,
        couponCode: appliedCoupon?.code,
      });
      const order = result?.order ?? result;
      const redirectUrl = result?.redirectUrl;

      if (redirectUrl) {
        toast.success(`Order ${order.orderNumber} placed — redirecting to payment…`);
        window.location.assign(redirectUrl);
        return;
      }

      await clearCart();
      toast.success(`Order ${order.orderNumber} placed successfully`);
      navigate(`/account/orders/${order.orderNumber}`, { replace: true });
    } catch (err) {
      setSubmitError(err.message || 'Could not place order. Please try again.');
      setSubmitting(false);
    }
  };

  const selectedAddress = addresses.find((a) => a.id === selectedAddressId);

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />

      <div className="max-w-[1440px] mx-auto px-6 py-10">
        <div className="mb-8">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-1">
            Step 2 of 2 · Review &amp; Confirm
          </p>
          <h1 className="font-['Anton'] text-5xl md:text-6xl uppercase tracking-tight">Checkout</h1>
        </div>

        {cartLoading && items.length === 0 ? (
          <div className="text-center py-24 text-black/40 text-sm uppercase tracking-wider">Loading…</div>
        ) : items.length === 0 ? (
          <EmptyCartNotice />
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-8 items-start">
            <div className="space-y-5">
              <Section title="Shipping Address" stepNum={1}>
                <AddressPicker
                  addresses={addresses}
                  loading={addressesLoading}
                  error={addressesError}
                  selectedId={selectedAddressId}
                  onSelect={setSelectedAddressId}
                />
              </Section>

              <Section title="Items" stepNum={2}>
                <CartReview items={items} currency={currency} />
                <div className="mt-3 text-[11px] text-black/50">
                  Need changes?{' '}
                  <Link to="/cart" className="underline hover:text-[#E83354]">Back to cart</Link>
                </div>
              </Section>

              <Section title="Payment Method" stepNum={3}>
                <PaymentPicker value={paymentMethod} onChange={setPaymentMethod} />
              </Section>

              <Section title="Order Notes" stepNum={4} optional>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  maxLength={500}
                  rows={3}
                  placeholder="Delivery instructions, gift message, etc."
                  className="w-full bg-white border border-black/15 px-3 py-2 text-sm focus:outline-none focus:border-black"
                />
                <div className="text-[10px] text-black/40 text-right mt-1">{notes.length}/500</div>
              </Section>
            </div>

            <aside className="lg:sticky lg:top-20">
              <div className="bg-[#0A0A0A] text-white p-6">
                <h2 className="font-['Anton'] text-2xl uppercase tracking-wider mb-6">Summary</h2>

                <div className="space-y-3 mb-4 text-sm">
                  <Row label={`Subtotal (${cartCount} ${cartCount === 1 ? 'item' : 'items'})`} value={formatPrice(subtotal, currency)} />
                  <Row label="Shipping" value={shippingCost === 0 ? 'Free' : formatPrice(shippingCost, currency)} />
                  <Row label="Tax" value={formatPrice(taxTotal, currency)} note="VAT not applied" />
                  {appliedCoupon && (
                    <Row label={`Discount · ${appliedCoupon.code}`} value={`− ${formatPrice(discountTotal, currency)}`} />
                  )}
                </div>

                <div className="mb-6">
                  {appliedCoupon ? (
                    <div className="flex items-center justify-between gap-2 border border-[#E83354]/50 bg-[#E83354]/10 px-3 py-2.5">
                      <div className="min-w-0">
                        <p className="text-[11px] font-bold tracking-[0.1em] uppercase text-[#E83354]">✓ {appliedCoupon.code}</p>
                        <p className="text-[10px] text-white/50">−{formatPrice(discountTotal, currency)} applied</p>
                      </div>
                      <button
                        type="button"
                        onClick={removeCoupon}
                        className="text-[10px] font-bold tracking-[0.1em] uppercase text-white/50 hover:text-white"
                      >
                        Remove
                      </button>
                    </div>
                  ) : (
                    <>
                      <div className="flex gap-2">
                        <input
                          type="text"
                          value={couponInput}
                          onChange={(e) => setCouponInput(e.target.value.toUpperCase())}
                          onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); applyCoupon(); } }}
                          placeholder="Coupon code"
                          className="flex-1 min-w-0 bg-white text-black text-sm px-3 py-2 focus:outline-none"
                        />
                        <button
                          type="button"
                          onClick={applyCoupon}
                          disabled={couponLoading || !couponInput.trim()}
                          className="text-[11px] font-bold tracking-[0.1em] uppercase border border-white/30 px-4 hover:bg-white hover:text-black transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                        >
                          {couponLoading ? '…' : 'Apply'}
                        </button>
                      </div>
                      {couponError && <p className="text-[10px] text-[#E83354] mt-1.5">{couponError}</p>}
                    </>
                  )}
                </div>

                <div className="border-t border-white/15 pt-4 mb-6">
                  <div className="flex justify-between items-baseline">
                    <span className="text-[11px] font-bold tracking-[0.1em] uppercase text-white/60">Total</span>
                    <span className="font-['Anton'] text-3xl">{formatPrice(grandTotal, currency)}</span>
                  </div>
                </div>

                {blockedItems.length > 0 && (
                  <p className="bg-[#E83354]/15 border-l-4 border-[#E83354] px-3 py-2 text-[11px] mb-4">
                    {blockedItems.length} item(s) in your cart cannot be ordered.{' '}
                    <Link to="/cart" className="underline">Fix in cart</Link>.
                  </p>
                )}

                {submitError && (
                  <p className="bg-[#E83354]/15 border-l-4 border-[#E83354] px-3 py-2 text-[11px] mb-4">
                    {submitError}
                  </p>
                )}

                <button
                  onClick={askConfirm}
                  disabled={!canPlaceOrder}
                  className="w-full bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#c82244] transition-colors mb-3 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-[#E83354]"
                >
                  {submitting ? 'Placing Order…' : 'Place Order'}
                </button>

                <Link
                  to="/cart"
                  className="block text-center w-full border border-white/20 text-white text-[11px] font-bold tracking-[0.1em] uppercase py-3 hover:border-white/50 transition-all"
                >
                  Back to Cart
                </Link>

                <p className="mt-5 text-[10px] text-white/30 text-center leading-relaxed">
                  {paymentMethod === 'COD' && 'COD orders are confirmed at delivery. You can cancel any time before we mark the order as processing.'}
                  {paymentMethod === 'VNPAY' && 'You will be redirected to VNPAY sandbox to complete payment. Stock is reserved for 15 minutes.'}
                  {paymentMethod === 'STRIPE' && 'You will be redirected to Stripe Checkout. Amount will be charged in USD using a locked exchange rate.'}
                </p>
              </div>
            </aside>
          </div>
        )}
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title="Confirm your order?"
        message={buildConfirmMessage({ cartCount, grandTotal, currency, selectedAddress, paymentMethod })}
        confirmLabel={paymentMethod === 'COD' ? 'Place Order' : 'Place Order & Pay'}
        cancelLabel="Review Again"
        onCancel={() => setConfirmOpen(false)}
        onConfirm={doPlaceOrder}
      />

      <FooterFull />
    </div>
  );
}

function Section({ title, stepNum, optional, children }) {
  return (
    <section className="bg-white p-5">
      <div className="flex items-baseline gap-3 mb-4">
        <span className="font-['Anton'] text-2xl text-black/30">{String(stepNum).padStart(2, '0')}</span>
        <h2 className="font-['Anton'] text-xl uppercase tracking-tight">{title}</h2>
        {optional && <span className="text-[10px] font-bold tracking-wider uppercase text-black/40">Optional</span>}
      </div>
      {children}
    </section>
  );
}

function AddressPicker({ addresses, loading, error, selectedId, onSelect }) {
  if (loading) return <p className="text-sm text-black/40">Loading addresses…</p>;
  if (error) return <p className="text-sm text-[#E83354]">{error}</p>;
  if (addresses.length === 0) {
    return (
      <div className="border border-dashed border-black/15 px-5 py-8 text-center">
        <p className="text-sm text-black/60 mb-3">No saved addresses yet.</p>
        <Link
          to="/account/addresses"
          className="inline-block text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
        >
          + Add Address
        </Link>
      </div>
    );
  }
  return (
    <div className="space-y-2">
      {addresses.map((a) => {
        const isSel = a.id === selectedId;
        return (
          <label
            key={a.id}
            className={`block border p-4 cursor-pointer transition-colors ${
              isSel ? 'border-[#E83354] bg-[#E83354]/5' : 'border-black/15 hover:border-black/40'
            }`}
          >
            <div className="flex items-start gap-3">
              <input
                type="radio"
                name="address"
                checked={isSel}
                onChange={() => onSelect(a.id)}
                className="mt-1 accent-[#E83354]"
              />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap mb-1">
                  {a.label && (
                    <span className="text-[10px] font-bold tracking-wider uppercase bg-black/8 px-2 py-0.5">{a.label}</span>
                  )}
                  {a.isDefault && (
                    <span className="text-[10px] font-bold tracking-wider uppercase bg-[#E83354] text-white px-2 py-0.5">Default</span>
                  )}
                </div>
                <p className="font-bold text-sm">{a.recipient}</p>
                <p className="text-xs text-black/60 mb-1">{a.phone}</p>
                <p className="text-xs text-black/70 leading-relaxed">
                  {a.line1}
                  {a.ward ? `, ${a.ward}` : ''}, {a.district}, {a.city}
                  {a.postalCode ? ` ${a.postalCode}` : ''} · {a.country}
                </p>
              </div>
            </div>
          </label>
        );
      })}
      <Link
        to="/account/addresses"
        className="block text-center text-[11px] font-bold tracking-[0.15em] uppercase border border-dashed border-black/30 py-3 hover:border-black hover:bg-black/5 transition-colors"
      >
        + Manage Addresses
      </Link>
    </div>
  );
}

function CartReview({ items, currency }) {
  return (
    <ul className="divide-y divide-black/8">
      {items.map((item) => (
        <li key={item.id ?? `g-${item.variantId}`} className="flex gap-3 py-3">
          <div className="w-14 h-16 flex-shrink-0 bg-black/5 overflow-hidden">
            {item.imageUrl && (
              <img src={item.imageUrl} alt={item.productName} className="w-full h-full object-cover" />
            )}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-bold uppercase tracking-wider truncate">{item.productName}</p>
            <p className="text-[11px] text-black/50">
              {item.size}{item.color ? ` · ${item.color}` : ''} · Qty {item.quantity}
            </p>
          </div>
          <span className="text-sm font-bold whitespace-nowrap">
            {formatPrice(item.lineTotal, item.currency ?? currency)}
          </span>
        </li>
      ))}
    </ul>
  );
}

function PaymentPicker({ value, onChange }) {
  const Option = ({ id, title, subtitle }) => {
    const isSel = value === id;
    return (
      <label className={`flex items-start gap-3 border p-4 cursor-pointer transition-colors ${
        isSel ? 'border-[#E83354] bg-[#E83354]/5' : 'border-black/15 hover:border-black/40'
      }`}>
        <input
          type="radio"
          name="payment"
          checked={isSel}
          onChange={() => onChange(id)}
          className="mt-1 accent-[#E83354]"
        />
        <div>
          <p className="font-bold text-sm uppercase tracking-wider">{title}</p>
          <p className="text-[11px] text-black/60 mt-0.5">{subtitle}</p>
        </div>
      </label>
    );
  };
  return (
    <div className="space-y-2">
      <Option id="COD" title="Cash on Delivery" subtitle="Pay with cash when your order is delivered." />
      <Option id="VNPAY" title="VNPAY · ATM / QR / Wallet" subtitle="Pay in VND via VNPAY sandbox gateway." />
      <Option id="STRIPE" title="Stripe · International Card" subtitle="Pay in USD via Stripe Checkout. FX locked at order time." />
    </div>
  );
}

function EmptyCartNotice() {
  return (
    <div className="text-center py-24 bg-white">
      <p className="font-['Anton'] text-3xl uppercase mb-4">Your cart is empty</p>
      <p className="text-black/50 mb-8">Add items before checking out.</p>
      <Link
        to="/shop"
        className="inline-block bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase px-12 py-4 hover:bg-[#E83354] transition-colors"
      >
        Shop Now
      </Link>
    </div>
  );
}

function Row({ label, value, note }) {
  return (
    <div className="flex justify-between items-baseline">
      <span className="text-white/60">{label}</span>
      <span className="text-right">
        <span className="font-bold">{value}</span>
        {note && <span className="block text-[10px] text-white/30">{note}</span>}
      </span>
    </div>
  );
}

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
}

function buildConfirmMessage({ cartCount, grandTotal, currency, selectedAddress, paymentMethod }) {
  const head = `${cartCount} ${cartCount === 1 ? 'item' : 'items'} · ${formatPrice(grandTotal, currency)}`;
  const ship = selectedAddress
    ? ` — shipping to ${selectedAddress.recipient}, ${selectedAddress.district}, ${selectedAddress.city}.`
    : '.';
  const tail =
    paymentMethod === 'VNPAY'
      ? ' You will be redirected to VNPAY sandbox to complete payment.'
      : paymentMethod === 'STRIPE'
        ? ' You will be redirected to Stripe Checkout (USD).'
        : ' Pay COD on delivery.';
  return head + ship + tail;
}
