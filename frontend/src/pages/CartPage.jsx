import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import { useCart } from '../context/CartContext';
import { products } from '../data/products';

export default function CartPage() {
  const {
    items,
    subtotal,
    currency,
    isLoading,
    isAuthenticated,
    error,
    updateQuantity,
    removeItem,
  } = useCart();
  const navigate = useNavigate();

  // Per-item action errors (e.g., "stock exceeded") shown inline without blowing up the page.
  const [actionError, setActionError] = useState(null);

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

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />

      <div className="max-w-[1440px] mx-auto px-6 py-10">
        <div className="mb-8">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-1">
            {items.length} {items.length === 1 ? 'item' : 'items'}
          </p>
          <h1 className="font-['Anton'] text-5xl md:text-6xl uppercase tracking-tight">Your Cart</h1>
        </div>

        {!isAuthenticated && items.length > 0 && (
          <div className="mb-6 bg-white border-l-4 border-[#E83354] px-4 py-3 flex items-center justify-between">
            <p className="text-xs text-black/70">
              <span className="font-bold uppercase tracking-wider">Sign in</span> to save this cart across devices.
            </p>
            <button
              onClick={() => navigate('/login')}
              className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black px-3 py-1.5 hover:bg-black hover:text-white transition-colors"
            >
              Sign In
            </button>
          </div>
        )}

        {error && (
          <div className="mb-6 bg-[#E83354]/10 border border-[#E83354]/30 px-4 py-3 text-xs text-[#E83354]">
            {error}
          </div>
        )}
        {actionError && (
          <div className="mb-6 bg-[#E83354]/10 border border-[#E83354]/30 px-4 py-3 text-xs text-[#E83354]">
            {actionError}
          </div>
        )}

        {isLoading && items.length === 0 ? (
          <div className="text-center py-24 text-black/40 text-sm uppercase tracking-wider">Loading cart…</div>
        ) : items.length === 0 ? (
          <div className="text-center py-24">
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
          <div className="flex gap-8 items-start">
            {/* CART ITEMS */}
            <div className="flex-1 space-y-4">
              <div className="hidden md:grid grid-cols-[auto_1fr_auto_auto_auto] gap-6 items-center pb-3 border-b border-black/15">
                {['', 'Product', 'Size / Color', 'Qty', 'Total'].map((h) => (
                  <span key={h} className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">{h}</span>
                ))}
              </div>

              {items.map((item) => {
                const key = item.id ?? `guest-${item.variantId}`;
                const navigateToProduct = () => {
                  if (item.productSlug) navigate(`/product/${item.productSlug}`);
                };
                return (
                  <div
                    key={key}
                    className="bg-white p-4 md:p-6 flex flex-col md:grid md:grid-cols-[80px_1fr_auto_auto_auto] gap-4 items-start md:items-center"
                  >
                    <div
                      className="w-20 h-24 overflow-hidden cursor-pointer flex-shrink-0 bg-black/5"
                      onClick={navigateToProduct}
                    >
                      {item.imageUrl && (
                        <img src={item.imageUrl} alt={item.productName} className="w-full h-full object-cover" />
                      )}
                    </div>

                    <div>
                      <h3
                        className="font-bold text-sm uppercase tracking-wider cursor-pointer hover:text-[#E83354] transition-colors mb-1"
                        onClick={navigateToProduct}
                      >
                        {item.productName ?? 'Product'}
                      </h3>
                      <div className="flex items-center gap-2 flex-wrap">
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
                          onClick={() => navigate('/try-on')}
                          className="text-[9px] font-bold tracking-wider border border-[#E83354] text-[#E83354] px-2 py-0.5 uppercase hover:bg-[#E83354] hover:text-white transition-all"
                        >
                          Try On
                        </button>
                      </div>
                      <p className="text-[11px] text-black/50 mt-1">
                        {formatPrice(item.unitPrice, item.currency ?? currency)} each
                      </p>
                      <StockBadge status={item.stockStatus} stock={item.stockQuantity} />
                    </div>

                    <div className="hidden md:block" />

                    <div className="flex items-center border border-black/15">
                      <button
                        onClick={() => handleQty(item, item.quantity - 1)}
                        disabled={item.quantity <= 1}
                        className="w-9 h-9 text-lg font-bold flex items-center justify-center hover:bg-black hover:text-white transition-all disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-black"
                      >
                        −
                      </button>
                      <span className="w-10 text-center text-sm font-bold">{item.quantity}</span>
                      <button
                        onClick={() => handleQty(item, item.quantity + 1)}
                        className="w-9 h-9 text-lg font-bold flex items-center justify-center hover:bg-black hover:text-white transition-all"
                      >
                        +
                      </button>
                    </div>

                    <div className="flex flex-col items-end gap-2">
                      <span className="font-['Anton'] text-xl">
                        {formatPrice(item.lineTotal, item.currency ?? currency)}
                      </span>
                      <button
                        onClick={() => handleRemove(item)}
                        className="text-[10px] font-bold tracking-wider uppercase text-black/30 hover:text-[#E83354] transition-colors"
                      >
                        Remove
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            {/* CHECKOUT SUMMARY */}
            <aside className="w-72 flex-shrink-0 sticky top-20">
              <div className="bg-[#0A0A0A] text-white p-6">
                <h2 className="font-['Anton'] text-2xl uppercase tracking-wider mb-6">Order Summary</h2>

                <div className="space-y-3 mb-6 text-sm">
                  <div className="flex justify-between">
                    <span className="text-white/60">Subtotal</span>
                    <span>{formatPrice(subtotal, currency)}</span>
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
                  <div className="flex justify-between items-center">
                    <span className="text-[11px] font-bold tracking-[0.1em] uppercase text-white/60">Subtotal</span>
                    <span className="font-['Anton'] text-3xl">{formatPrice(subtotal, currency)}</span>
                  </div>
                </div>

                <div className="flex mb-4">
                  <input
                    type="text"
                    placeholder="Promo code"
                    disabled
                    className="flex-1 bg-white/10 border border-white/20 text-white text-sm px-3 py-2.5 focus:outline-none focus:border-white/50 placeholder:text-white/30 disabled:opacity-50"
                  />
                  <button
                    disabled
                    className="bg-white/15 text-white text-[10px] font-bold tracking-wider px-4 hover:bg-white/25 transition-colors disabled:opacity-50"
                  >
                    APPLY
                  </button>
                </div>

                <button
                  disabled={checkoutDisabled}
                  className="w-full bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#c82244] transition-colors mb-3 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-[#E83354]"
                >
                  {hasBlockedItems ? 'Resolve Stock Issues' : 'Place Order'}
                </button>

                <button
                  onClick={() => navigate('/shop')}
                  className="w-full border border-white/20 text-white text-[11px] font-bold tracking-[0.1em] uppercase py-3 hover:border-white/50 transition-all"
                >
                  Continue Shopping
                </button>

                <div className="mt-4 space-y-2">
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

        {/* You May Also Like — still using local data; will be wired to /products in a later phase. */}
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

      <FooterFull />
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
    <span className={`inline-block mt-1 text-[10px] font-bold tracking-wider uppercase px-2 py-0.5 ${styles[status]}`}>
      {label}
    </span>
  );
}

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'VND') return `${num.toLocaleString('vi-VN')} ₫`;
  return `${currency || '$'} ${num.toFixed(2)}`;
}
