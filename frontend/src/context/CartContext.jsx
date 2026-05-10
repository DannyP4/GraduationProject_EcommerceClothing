import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useAuth } from './AuthContext';
import * as cartService from '../services/cartService';

const CartContext = createContext(null);

const STORAGE_KEY = 'cart.guest';
const EMPTY_CART = { id: null, items: [], itemCount: 0, subtotal: 0, currency: 'VND' };

// Recompute totals on every read so stale localStorage docs never serve wrong values.
function normalizeGuestCart(items) {
  const list = items.map((i) => ({
    ...i,
    lineTotal: Number(i.unitPrice ?? 0) * i.quantity,
  }));
  return {
    id: null,
    items: list,
    itemCount: list.reduce((sum, i) => sum + i.quantity, 0),
    subtotal: list.reduce((sum, i) => sum + i.lineTotal, 0),
    currency: list[0]?.currency ?? 'VND',
  };
}

function readGuestCart() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return EMPTY_CART;
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed?.items)) return EMPTY_CART;
    return normalizeGuestCart(parsed.items);
  } catch {
    return EMPTY_CART;
  }
}

function writeGuestCart(items) {
  if (!items || items.length === 0) {
    localStorage.removeItem(STORAGE_KEY);
    return;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ items }));
}

export function CartProvider({ children }) {
  const { status } = useAuth();
  const [cart, setCart] = useState(EMPTY_CART);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  // Track previous auth status to detect the unauth → auth transition 
  const prevStatusRef = useRef(status);

  useEffect(() => {
    let cancelled = false;
    const prev = prevStatusRef.current;
    prevStatusRef.current = status;

    async function hydrate() {
      if (status === 'authenticated') {
        setIsLoading(true);
        setError(null);
        try {
          const guest = readGuestCart();
          const justLoggedIn = prev !== 'authenticated';
          if (justLoggedIn && guest.items.length > 0) {
            const merged = await cartService.mergeCart(
              guest.items.map((i) => ({ variantId: i.variantId, quantity: i.quantity }))
            );
            if (cancelled) return;
            localStorage.removeItem(STORAGE_KEY);
            setCart(merged);
          } else {
            const fresh = await cartService.getCart();
            if (cancelled) return;
            setCart(fresh);
          }
        } catch (err) {
          if (!cancelled) setError(err.message || 'Failed to load cart');
        } finally {
          if (!cancelled) setIsLoading(false);
        }
        return;
      }

      // unauthenticated or 'loading' — show guest cart, avoid flicker during auth bootstrap.
      setCart(readGuestCart());
    }

    hydrate();
    return () => { cancelled = true; };
  }, [status]);

  const addItem = useCallback(async (payload) => {
    const { variantId, quantity = 1, ...meta } = payload;
    if (!variantId) throw new Error('addItem requires variantId');

    if (status === 'authenticated') {
      const updated = await cartService.addItem({ variantId, quantity });
      setCart(updated);
      return updated;
    }

    setCart((prev) => {
      const existingIdx = prev.items.findIndex((i) => i.variantId === variantId);
      const nextItems = existingIdx >= 0
        ? prev.items.map((i, idx) =>
          idx === existingIdx ? { ...i, quantity: i.quantity + quantity } : i)
        : [...prev.items, { variantId, quantity, ...meta }];
      writeGuestCart(nextItems);
      return normalizeGuestCart(nextItems);
    });
    return null;
  }, [status]);

  // Auth mode keys items by server itemId
  const updateQuantity = useCallback(async (item, newQuantity) => {
    if (status === 'authenticated') {
      const updated = await cartService.updateItem(item.id, newQuantity);
      setCart(updated);
      return updated;
    }
    setCart((prev) => {
      const nextItems = newQuantity <= 0
        ? prev.items.filter((i) => i.variantId !== item.variantId)
        : prev.items.map((i) =>
          i.variantId === item.variantId ? { ...i, quantity: newQuantity } : i);
      writeGuestCart(nextItems);
      return normalizeGuestCart(nextItems);
    });
    return null;
  }, [status]);

  const removeItem = useCallback(async (item) => {
    if (status === 'authenticated') {
      const updated = await cartService.removeItem(item.id);
      setCart(updated);
      return updated;
    }
    setCart((prev) => {
      const nextItems = prev.items.filter((i) => i.variantId !== item.variantId);
      writeGuestCart(nextItems);
      return normalizeGuestCart(nextItems);
    });
    return null;
  }, [status]);

  const clearCart = useCallback(async () => {
    if (status === 'authenticated') {
      const updated = await cartService.clearCart();
      setCart(updated);
      return;
    }
    localStorage.removeItem(STORAGE_KEY);
    setCart(EMPTY_CART);
  }, [status]);

  const value = {
    cart,
    items: cart.items,
    cartCount: cart.itemCount,
    subtotal: cart.subtotal,
    currency: cart.currency,
    isLoading,
    error,
    isAuthenticated: status === 'authenticated',
    addItem,
    updateQuantity,
    removeItem,
    clearCart,
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export const useCart = () => {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
};
