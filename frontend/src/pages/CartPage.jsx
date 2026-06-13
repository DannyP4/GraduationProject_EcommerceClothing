import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import QuantityStepper from '../components/QuantityStepper';
import ConfirmDialog from '../components/ConfirmDialog';
import { useCart } from '../context/CartContext';
import { getProducts, getSimilarToProducts } from '../services/productService';
import { Carousel } from '../components/RecommendationRow';
import { formatPrice } from '../lib/format';
import { colorLabel } from '../lib/labels';

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
  const { t, i18n } = useTranslation();

  const [actionError, setActionError] = useState(null);
  const [confirmClear, setConfirmClear] = useState(false);
  const [recs, setRecs] = useState([]);
  const cartIdsKey = [...new Set(items.map((i) => i.productId).filter(Boolean))]
    .sort((a, b) => a - b)
    .join(',');

  useEffect(() => {
    let cancelled = false;
    const ids = cartIdsKey ? cartIdsKey.split(',').map(Number) : [];
    // Cart-based "you may also like"; fall back to popular when the cart is empty.
    const load = ids.length
      ? getSimilarToProducts(ids, 12)
      : getProducts({ size: 12, sort: 'POPULAR' }).then((d) => d?.content ?? []);
    Promise.resolve(load)
      .then((data) => { if (!cancelled) setRecs(Array.isArray(data) ? data : []); })
      .catch(() => { if (!cancelled) setRecs([]); });
    return () => { cancelled = true; };
  }, [cartIdsKey, i18n.language]);

  const hasBlockedItems = items.some(
    (i) => i.stockStatus === 'OUT_OF_STOCK' || i.stockStatus === 'UNAVAILABLE'
  );
  const checkoutDisabled = items.length === 0 || hasBlockedItems;

  const handleQty = async (item, nextQty) => {
    setActionError(null);
    try {
      await updateQuantity(item, nextQty);
    } catch (err) {
      setActionError(err.message || t('cartPage.errors.updateQty'));
    }
  };

  const handleRemove = async (item) => {
    setActionError(null);
    try {
      await removeItem(item);
    } catch (err) {
      setActionError(err.message || t('cartPage.errors.removeItem'));
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
      setActionError(err.message || t('cartPage.errors.clearCart'));
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
                {t('cartPage.itemCount', { count: cartCount })}
              </span>
              {!isAuthenticated && items.length > 0 && (
                <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">
                  · {t('cartPage.guestCart')}
                </span>
              )}
            </div>
            <h1 className="cart-title font-['Anton'] text-5xl md:text-6xl uppercase tracking-tight">{t('cartPage.title')}</h1>
          </div>
          {items.length > 0 && (
            <button
              onClick={askClearAll}
              className="text-[10px] font-bold tracking-[0.15em] uppercase border border-[#E83354] text-[#E83354] bg-[#E83354]/5 px-3 py-2 hover:bg-[#E83354] hover:text-white transition-colors"
            >
              {t('cartPage.clearAll')}
            </button>
          )}
        </div>

        {!isAuthenticated && items.length > 0 && (
          <div className="mb-6 bg-white border-l-4 border-[#E83354] px-4 py-3 flex items-center justify-between">
            <p className="text-xs text-black/70">
              <span className="font-bold uppercase tracking-wider">{t('cartPage.signInPromptLead')}</span> {t('cartPage.signInPromptRest')}
            </p>
            <button
              onClick={() => navigate('/login', { state: { from: '/cart' } })}
              className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black px-3 py-1.5 hover:bg-black hover:text-white transition-colors"
            >
              {t('cartPage.signIn')}
            </button>
          </div>
        )}

        {error && <Banner type="error">{error}</Banner>}
        {actionError && <Banner type="error">{actionError}</Banner>}

        {isLoading && items.length === 0 ? (
          <div className="text-center py-24 text-black/40 text-sm uppercase tracking-wider">{t('cartPage.loading')}</div>
        ) : items.length === 0 ? (
          <div className="text-center py-24 bg-white">
            <p className="font-['Anton'] text-3xl uppercase mb-4">{t('cartPage.empty.title')}</p>
            <p className="text-black/50 mb-8">{t('cartPage.empty.subtitle')}</p>
            <button
              onClick={() => navigate('/shop')}
              className="bg-black text-white text-[12px] font-bold tracking-[0.15em] uppercase px-12 py-4 hover:bg-[#E83354] transition-colors"
            >
              {t('cartPage.empty.shopNow')}
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_320px] gap-8 items-start">
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
                <h2 className="font-['Anton'] text-2xl uppercase tracking-wider mb-6">{t('cartPage.summary.title')}</h2>

                <div className="space-y-3 mb-6 text-sm">
                  <div className="flex justify-between">
                    <span className="text-white/60">{t('cartPage.summary.subtotal', { count: cartCount })}</span>
                    <span className="font-bold">{formatPrice(subtotal, currency)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-white/60">{t('cartPage.summary.shipping')}</span>
                    <span className="text-white/40 text-xs">{t('cartPage.summary.calculatedAtCheckout')}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-white/60">{t('cartPage.summary.tax')}</span>
                    <span className="text-white/40 text-xs">{t('cartPage.summary.calculatedAtCheckout')}</span>
                  </div>
                </div>

                <div className="border-t border-white/15 pt-4 mb-6">
                  <div className="flex justify-between items-baseline">
                    <span className="text-[11px] font-bold tracking-[0.1em] uppercase text-white/60">{t('cartPage.summary.total')}</span>
                    <span className="font-['Anton'] text-3xl">{formatPrice(subtotal, currency)}</span>
                  </div>
                  <p className="text-[10px] text-white/30 mt-1">{t('cartPage.summary.exclShippingTax')}</p>
                </div>

                <button
                  onClick={handleCheckout}
                  disabled={checkoutDisabled}
                  className="w-full bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#c82244] transition-colors mb-3 disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-[#E83354]"
                >
                  {hasBlockedItems
                    ? t('cartPage.checkout.resolveStock')
                    : !isAuthenticated
                      ? t('cartPage.checkout.signInToCheckout')
                      : t('cartPage.checkout.placeOrder')}
                </button>

                <button
                  onClick={() => navigate('/shop')}
                  className="w-full border border-white/20 text-white text-[11px] font-bold tracking-[0.1em] uppercase py-3 hover:border-white/50 transition-all"
                >
                  {t('cartPage.continueShopping')}
                </button>

                <div className="mt-5 space-y-1.5">
                  {[
                    t('cartPage.perks.secureCheckout'),
                    t('cartPage.perks.freeReturns'),
                    t('cartPage.perks.multiplePayments'),
                  ].map((f) => (
                    <div key={f} className="flex items-center gap-2 text-[10px] text-white/40">
                      <span className="text-green-400">✓</span> {f}
                    </div>
                  ))}
                </div>
              </div>
            </aside>
          </div>
        )}

        {recs.length > 0 && <Carousel title={t('cartPage.youMayAlsoLike')} items={recs} />}
      </div>

      <ConfirmDialog
        open={confirmClear}
        title={t('cartPage.confirmClear.title')}
        message={t('cartPage.confirmClear.message', { count: cartCount })}
        confirmLabel={t('cartPage.confirmClear.confirm')}
        cancelLabel={t('cartPage.confirmClear.cancel')}
        tone="danger"
        onCancel={() => setConfirmClear(false)}
        onConfirm={doClearAll}
      />

      <FooterFull />
    </div>
  );
}

function CartItemRow({ item, currency, onQtyChange, onRemove, onProductClick, onTryOn }) {
  const { t } = useTranslation();
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
              title={item.productName ?? undefined}
              className="font-bold text-sm uppercase tracking-wider cursor-pointer hover:text-[#E83354] transition-colors truncate"
            >
              {item.productName ?? t('cartPage.item.fallbackName')}
            </h3>
            <div className="flex items-center gap-1.5 mt-1.5 flex-wrap">
              {item.size && (
                <span className="text-[10px] font-bold tracking-wider bg-black/8 px-2 py-0.5 uppercase">
                  {item.size}
                </span>
              )}
              {item.color && (
                <span className="text-[10px] font-bold tracking-wider bg-black/8 px-2 py-0.5 uppercase">
                  {colorLabel(t, item.color)}
                </span>
              )}
              <button
                onClick={onTryOn}
                className="text-[9px] font-bold tracking-wider border border-[#E83354] text-[#E83354] px-2 py-0.5 uppercase hover:bg-[#E83354] hover:text-white transition-all"
              >
                {t('cartPage.item.tryOn')}
              </button>
            </div>
            <p className="text-[11px] text-black/50 mt-1.5 flex items-center gap-1.5 flex-wrap">
              {item.originalUnitPrice != null && (
                <span className="line-through text-black/30">
                  {formatPrice(item.originalUnitPrice, item.currency ?? currency)}
                </span>
              )}
              <span className={item.originalUnitPrice != null ? 'text-[#E83354] font-bold' : ''}>
                {formatPrice(item.unitPrice, item.currency ?? currency)}
              </span>
              <span>{t('cartPage.item.each')}</span>
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
            aria-label={t('cartPage.item.removeAria', { name: item.productName })}
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6" />
              <path d="M10 11v6M14 11v6" />
            </svg>
            {t('cartPage.item.remove')}
          </button>
        </div>
      </div>
    </div>
  );
}

function StockBadge({ status, stock }) {
  const { t } = useTranslation();
  if (!status || status === 'IN_STOCK') return null;
  const styles = {
    LOW_STOCK: 'text-amber-700 bg-amber-100',
    OUT_OF_STOCK: 'text-[#E83354] bg-[#E83354]/10',
    UNAVAILABLE: 'text-black/60 bg-black/10',
  };
  const label = {
    LOW_STOCK: stock != null ? t('cartPage.stock.onlyLeft', { n: stock }) : t('cartPage.stock.low'),
    OUT_OF_STOCK: t('cartPage.stock.out'),
    UNAVAILABLE: t('cartPage.stock.unavailable'),
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
