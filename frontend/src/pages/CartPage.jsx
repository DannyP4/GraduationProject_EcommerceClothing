import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import QuantityStepper from '../components/QuantityStepper';
import ConfirmDialog from '../components/ConfirmDialog';
import { useCart } from '../context/CartContext';
import { products } from '../data/products';

export default function CartPage() {
  const {
    items,
    cartCount,
    subtotal,
    currency,
    isLoading,
    isAuthenticated,
    error,
    updateQuantity,
    removeItem,
    clearCart,
  } = useCart();
  const navigate = useNavigate();

  const [actionError, setActionError] = useState(null);
  const [confirmClear, setConfirmClear] = useState(false);


  const hasBlockedItems = items.some(
    (i) => i.stockStatus === 'OUT_OF_STOCK' || i.stockStatus === 'UNAVAILABLE'
  );
  const checkoutDisabled = items.length === 0 || hasBlockedItems;

  const suggestions = products.slice(4, 8);

  const handleQty = async (item, nextQty) => {
    setActionError(null);
    try {
      await updateQuantity(item, nextQty);
    } catch (err) {
      setActionError(err.message || 'Could not update quantity');
    }
  };

  const handleRemove = async (item) => {
    setActionError(null);
    try {
      await removeItem(item);
    } catch (err) {
      setActionError(err.message || 'Could not remove item');
    }
  };

  const askClearAll = () => {
    if (items.length === 0) return;
    setConfirmClear(true);
  };

  const doClearAll = async () => {
    setConfirmClear(false);
    setActionError(null);
    try {
      await clearCart();
    } catch (err) {
      setActionError(err.message || 'Could not clear cart');
    }
  };

  const handleCheckout = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: '/cart' } });
      return;
    }
    navigate('/checkout');
  };

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />

      <div className="max-w-[1440px] mx-auto px-6 py-10">
        {/* HEADER */}
        <div className="flex items-end justify-between gap-4 mb-8">
          <div>
            <div className="inline-flex items-center gap-2 mb-2">
              <span className="bg-[#E83354] text-white text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-1">
                {cartCount} {cartCount === 1 ? 'item' : 'items'}
              </span>
              {!isAuthenticated && items.length > 0 && (
                <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">
                  · guest cart
                </span>
              )}
            </div>
            <h1 className="font-['Anton'] text-5xl md:text-6xl uppercase tracking-tight">Your Cart</h1>
          </div>
          {items.length > 0 && (
            <button
              onClick={askClearAll}
              className="text-[10px] font-bold tracking-[0.15em] uppercase border border-[#E83354] text-[#E83354] bg-[#E83354]/5 px-3 py-2 hover:bg-[#E83354] hover:text-white transition-colors"
            >
              Clear All
            </button>
          )}
        </div>

        {!isAuthenticated && items.length > 0 && (
          <div className="mb-6 bg-white border-l-4 border-[#E83354] px-4 py-3 flex items-center justify-between">
            <p className="text-xs text-black/70">
              <span className="font-bold uppercase tracking-wider">Sign in</span> to save this cart across devices.
            </p>
            <button
              onClick={() => navigate('/login', { state: { from: '/cart' } })}
              className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black px-3 py-1.5 hover:bg-black hover:text-white transition-colors"
            >
              Sign In
            </button>
          </div>
        )}

        {error && <Banner type="error">{error}</Banner>}
        {actionError && <Banner type="error">{actionError}</Banner>}

        {isLoading && items.length === 0 ? (
          <div className="text-center py-24 text-black/40 text-sm uppercase tracking-wider">Loading cart…</div>
        ) : items.length === 0 ? (
          <div className="text-center py-24 bg-white">
            <p className="font-['Anton'] text-3xl uppercase mb-4">Your cart is empty</p>
            <p className="text-black/50 mb-8">Add some legendary pieces to get started.</p>
            <button
              onClick={() => navigate('/shop')}
              className="bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase px-12 py-4 hover:bg-[#E83354] transition-colors"
            >
              Shop Now
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-8 items-start">
            {/* CART ITEMS */}
            <div className="space-y-3">
              {items.map((item) => (
                <CartItemRow
                  key={item.id ?? `guest-${item.variantId}`}
                  item={item}
                  currency={currency}
                  onQtyChange={(qty) => handleQty(item, qty)}
                  onRemove={() => handleRemove(item)}
                  onProductClick={() => item.productSlug && navigate(`/product/${item.productSlug}`)}
                  onTryOn={() => navigate('/try-on')}
                />
              ))}
            </div>

            {/* SUMMARY */}
            <aside className="lg:sticky lg:top-20">
              <div className="bg-[#0A0A0A] text-white p-6">
                <h2 className="font-['Anton'] text-2xl uppercase tracking-wider mb-6">Order Summary</h2>

                <div className="space-y-3 mb-6 text-sm">
                  <div className="flex justify-between">
                    <span className="text-white/60">Subtotal ({cartCount} {cartCount === 1 ? 'item' : 'items'})</span>
                    <span className="font-bold">{formatPrice(subtotal, currency)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-white/60">Shipping</span>
                    <span className="text-white/40 text-xs">Calculated at checkout</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-white/60">Tax</span>
                    <span className="text-white/40 text-xs">Calculated at checkout</span>
                  </div>
                </div>

                <div className="border-t border-white/15 pt-4 mb-6">
                  <div className="flex justify-between items-baseline">
                    <span className="text-[11px] font-bold tracking-[0.1em] uppercase text-white/60">Total</span>
                    <span className="font-['Anton'] text-3xl">{formatPrice(subtotal, currency)}</span>
                  </div>
                  <p className="text-[10px] text-white/30 mt-1">excl. shipping &amp; tax</p>
                </div>

                <button
                  onClick={handleCheckout}
                  disabled={checkoutDisabled}
                  className="w-full bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#c82244] transition-colors mb-3 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-[#E83354]"
                >
                  {hasBlockedItems
                    ? 'Resolve Stock Issues'
                    : !isAuthenticated
                      ? 'Sign in to Checkout'
                      : 'Place Order'}
                </button>

                <button
                  onClick={() => navigate('/shop')}
                  className="w-full border border-white/20 text-white text-[11px] font-bold tracking-[0.1em] uppercase py-3 hover:border-white/50 transition-all"
                >
                  Continue Shopping
                </button>

                <div className="mt-5 space-y-1.5">
                  {['Secure Checkout', 'Free Returns within 30 Days', 'Multiple Payment Options'].map((f) => (
                    <div key={f} className="flex items-center gap-2 text-[10px] text-white/40">
                      <span className="text-green-400">✓</span> {f}
                    </div>
                  ))}
                </div>
              </div>
            </aside>
          </div>
        )}

        <section className="mt-20">
          <div className="mb-8">
            <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-1">Suggested</p>
            <h2 className="font-['Anton'] text-4xl uppercase tracking-tight">You May Also Like</h2>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {suggestions.map((p) => (
              <div
                key={p.id}
                className="group bg-white cursor-pointer"
                onClick={() => navigate(`/product/${p.id}`)}
              >
                <div className="relative overflow-hidden" style={{ paddingTop: '125%' }}>
                  <img
                    src={p.images[0]}
                    alt={p.name}
                    className="absolute inset-0 w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                  />
                  {p.badge && (
                    <span className="absolute top-2 left-2 bg-[#E83354] text-white text-[9px] font-bold tracking-widest uppercase px-1.5 py-0.5">
                      {p.badge}
                    </span>
                  )}
                </div>
                <div className="p-3">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-black/40 mb-0.5">{p.category}</p>
                  <h3 className="text-xs font-bold uppercase tracking-wider mb-1">{p.name}</h3>
                  <span className="font-['Anton'] text-lg">${p.price}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <ConfirmDialog
        open={confirmClear}
        title="Clear all items?"
        message={`This will remove all ${cartCount} ${cartCount === 1 ? 'item' : 'items'} from your cart. This cannot be undone.`}
        confirmLabel="Clear Cart"
        cancelLabel="Keep Items"
        tone="danger"
        onCancel={() => setConfirmClear(false)}
        onConfirm={doClearAll}
      />

      <FooterFull />
    </div>
  );
}

function CartItemRow({ item, currency, onQtyChange, onRemove, onProductClick, onTryOn }) {
  return (
    <div className="bg-white p-4 md:p-5 flex gap-4 md:gap-5">
      <div
        className="w-24 h-28 md:w-28 md:h-32 overflow-hidden cursor-pointer flex-shrink-0 bg-black/5"
        onClick={onProductClick}
      >
        {item.imageUrl && (
          <img src={item.imageUrl} alt={item.productName} className="w-full h-full object-cover" />
        )}
      </div>

      <div className="flex-1 min-w-0 flex flex-col">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h3
              onClick={onProductClick}
              className="font-bold text-sm uppercase tracking-wider cursor-pointer hover:text-[#E83354] transition-colors truncate"
            >
              {item.productName ?? 'Product'}
            </h3>
            <div className="flex items-center gap-1.5 mt-1.5 flex-wrap">
              {item.size && (
                <span className="text-[10px] font-bold tracking-wider bg-black/8 px-2 py-0.5 uppercase">
                  {item.size}
                </span>
              )}
              {item.color && (
                <span className="text-[10px] font-bold tracking-wider bg-black/8 px-2 py-0.5 uppercase">
                  {item.color}
                </span>
              )}
              <button
                onClick={onTryOn}
                className="text-[9px] font-bold tracking-wider border border-[#E83354] text-[#E83354] px-2 py-0.5 uppercase hover:bg-[#E83354] hover:text-white transition-all"
              >
                Try On
              </button>
            </div>
            <p className="text-[11px] text-black/50 mt-1.5">
              {formatPrice(item.unitPrice, item.currency ?? currency)} each
            </p>
            <StockBadge status={item.stockStatus} stock={item.stockQuantity} />
          </div>

          <span className="font-['Anton'] text-xl whitespace-nowrap">
            {formatPrice(item.lineTotal, item.currency ?? currency)}
          </span>
        </div>

        <div className="mt-auto pt-3 flex items-center justify-between gap-3">
          <QuantityStepper
            value={item.quantity}
            max={item.stockQuantity ?? 99}
            onChange={onQtyChange}
          />
          <button
            onClick={onRemove}
            className="flex items-center gap-1.5 text-[10px] font-bold tracking-wider uppercase text-[#E83354] border border-[#E83354]/40 bg-[#E83354]/5 px-3 py-1.5 hover:bg-[#E83354] hover:text-white hover:border-[#E83354] transition-colors"
            aria-label={`Remove ${item.productName} from cart`}
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6" />
              <path d="M10 11v6M14 11v6" />
            </svg>
            Remove
          </button>
        </div>
      </div>
    </div>
  );
}

function StockBadge({ status, stock }) {
  if (!status || status === 'IN_STOCK') return null;
  const styles = {
    LOW_STOCK: 'text-amber-700 bg-amber-100',
    OUT_OF_STOCK: 'text-[#E83354] bg-[#E83354]/10',
    UNAVAILABLE: 'text-black/60 bg-black/10',
  };
  const label = {
    LOW_STOCK: stock != null ? `Only ${stock} left` : 'Low stock',
    OUT_OF_STOCK: 'Out of stock',
    UNAVAILABLE: 'No longer available',
  }[status];
  return (
    <span className={`inline-block mt-1.5 text-[10px] font-bold tracking-wider uppercase px-2 py-0.5 ${styles[status]}`}>
      {label}
    </span>
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
