import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useAuth } from './AuthContext';
import * as wishlistService from '../services/wishlistService';

const WishlistContext = createContext(null);

export function WishlistProvider({ children }) {
  const { status } = useAuth();
  const [ids, setIds] = useState(() => new Set());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let cancelled = false;

    if (status !== 'authenticated') {
      setIds(new Set());
      return;
    }

    setLoading(true);
    wishlistService.getWishlistIds()
      .then((list) => { if (!cancelled) setIds(new Set(list || [])); })
      .catch(() => { if (!cancelled) setIds(new Set()); })
      .finally(() => { if (!cancelled) setLoading(false); });

    return () => { cancelled = true; };
  }, [status]);

  const isWishlisted = useCallback((productId) => ids.has(productId), [ids]);

  const toggle = useCallback(async (productId) => {
    const res = await wishlistService.toggleWishlist(productId);
    setIds((prev) => {
      const next = new Set(prev);
      if (res.wishlisted) next.add(productId);
      else next.delete(productId);
      return next;
    });
    return res.wishlisted;
  }, []);

  const remove = useCallback(async (productId) => {
    await wishlistService.removeFromWishlist(productId);
    setIds((prev) => {
      const next = new Set(prev);
      next.delete(productId);
      return next;
    });
  }, []);

  const clear = useCallback(async () => {
    await wishlistService.clearWishlist();
    setIds(new Set());
  }, []);

  const value = {
    ids,
    count: ids.size,
    loading,
    isAuthenticated: status === 'authenticated',
    isWishlisted,
    toggle,
    remove,
    clear,
  };

  return <WishlistContext.Provider value={value}>{children}</WishlistContext.Provider>;
}

export const useWishlist = () => {
  const ctx = useContext(WishlistContext);
  if (!ctx) throw new Error('useWishlist must be used within WishlistProvider');
  return ctx;
};
