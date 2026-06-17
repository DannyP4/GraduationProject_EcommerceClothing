import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';
import { useWishlist } from '../context/WishlistContext';
import { useToast } from './Toast';
import HeartIcon from './HeartIcon';

export default function WishlistButton({ productId, productSlug, size = 17, className = '' }) {
  const { t } = useTranslation();
  const { status } = useAuth();
  const { isWishlisted, toggle } = useWishlist();
  const toast = useToast();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);

  const wished = isWishlisted(productId);

  const onClick = async (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (status !== 'authenticated') {
      toast.error(t('wishlist.signInRequired'));
      navigate('/login', { state: { from: `/product/${productSlug || productId}` } });
      return;
    }
    if (busy) return;
    setBusy(true);
    try {
      const nowWished = await toggle(productId);
      toast.success(nowWished ? t('wishlist.added') : t('wishlist.removed'));
    } catch {
      toast.error(t('wishlist.error'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={busy}
      aria-label={wished ? t('wishlist.remove') : t('wishlist.add')}
      aria-pressed={wished}
      className={`absolute top-2 right-2 z-10 w-8 h-8 flex items-center justify-center rounded-full bg-white/85 backdrop-blur transition-all hover:bg-white disabled:opacity-50 ${
        wished ? 'text-[#E83354]' : 'text-black/55 hover:text-[#E83354]'
      } ${className}`}
    >
      <HeartIcon filled={wished} size={size} />
    </button>
  );
}
