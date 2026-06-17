import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import ProductCard from '../components/ProductCard';
import ConfirmDialog from '../components/ConfirmDialog';
import { useWishlist } from '../context/WishlistContext';
import { useToast } from '../components/Toast';
import * as wishlistService from '../services/wishlistService';

export default function AccountWishlistPage() {
  const { t, i18n } = useTranslation();
  const { remove, clear } = useWishlist();
  const toast = useToast();
  const [items, setItems] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [clearOpen, setClearOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    wishlistService.getWishlist()
      .then((res) => { if (!cancelled) setItems(res || []); })
      .catch((err) => { if (!cancelled) setError(err.message || t('wishlist.loadError')); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [i18n.language, t]);

  const handleRemove = async (productId) => {
    try {
      await remove(productId);
      setItems((prev) => prev.filter((p) => p.id !== productId));
      toast.success(t('wishlist.removed'));
    } catch {
      toast.error(t('wishlist.error'));
    }
  };

  const doClear = async () => {
    setClearOpen(false);
    try {
      await clear();
      setItems([]);
      toast.success(t('wishlist.cleared'));
    } catch {
      toast.error(t('wishlist.error'));
    }
  };

  const count = items?.length ?? 0;

  return (
    <div>
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h2 className="font-['Anton'] text-3xl uppercase tracking-tight">{t('wishlist.heading')}</h2>
          <p className="text-xs text-black/50 mt-1">{t('wishlist.subtitle')}</p>
        </div>
        {count > 0 && (
          <button
            type="button"
            onClick={() => setClearOpen(true)}
            className="flex-shrink-0 text-[10px] font-bold tracking-[0.15em] uppercase border border-[#E83354] text-[#E83354] bg-[#E83354]/5 px-3 py-2 hover:bg-[#E83354] hover:text-white transition-colors"
          >
            {t('wishlist.clearAll')}
          </button>
        )}
      </div>

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs mb-4">{error}</div>
      )}

      {loading && !items ? (
        <p className="text-sm text-black/40">{t('accountPage.loading')}</p>
      ) : count === 0 ? (
        <Empty />
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {items.map((p) => (
            <div key={p.id} className="relative group">
              <ProductCard product={p} hideWishlist />
              <button
                type="button"
                onClick={() => handleRemove(p.id)}
                aria-label={t('wishlist.remove')}
                title={t('wishlist.remove')}
                className="absolute top-2 right-2 z-10 w-8 h-8 flex items-center justify-center rounded-full bg-white/90 backdrop-blur text-black/55 shadow-sm hover:bg-[#E83354] hover:text-white transition-all"
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                  <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={clearOpen}
        title={t('wishlist.clearConfirmTitle')}
        message={t('wishlist.clearConfirmMessage', { n: count })}
        confirmLabel={t('wishlist.clearAll')}
        cancelLabel={t('wishlist.clearCancel')}
        tone="danger"
        onCancel={() => setClearOpen(false)}
        onConfirm={doClear}
      />
    </div>
  );
}

function Empty() {
  const { t } = useTranslation();
  return (
    <div className="border border-dashed border-black/15 px-6 py-16 text-center">
      <p className="text-sm text-black/50 mb-4">{t('wishlist.empty')}</p>
      <Link
        to="/shop"
        className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
      >
        {t('wishlist.startShopping')}
      </Link>
    </div>
  );
}
