import { createContext, useContext, useReducer } from 'react';

const CartContext = createContext(null);

function cartReducer(state, action) {
  switch (action.type) {
    case 'ADD_ITEM': {
      const existing = state.find(
        (i) => i.id === action.item.id && i.size === action.item.size && i.color === action.item.color
      );
      if (existing) {
        return state.map((i) =>
          i.id === action.item.id && i.size === action.item.size && i.color === action.item.color
            ? { ...i, qty: i.qty + 1 }
            : i
        );
      }
      return [...state, { ...action.item, qty: 1 }];
    }
    case 'REMOVE_ITEM':
      return state.filter((i) => i.cartKey !== action.cartKey);
    case 'UPDATE_QTY':
      return state
        .map((i) => (i.cartKey === action.cartKey ? { ...i, qty: action.qty } : i))
        .filter((i) => i.qty > 0);
    default:
      return state;
  }
}

export function CartProvider({ children }) {
  const [items, dispatch] = useReducer(cartReducer, [
    // Default cart items for demo
    {
      cartKey: 'varsity-bomber-M-Black',
      id: 'varsity-bomber',
      name: 'Varsity Bomber',
      price: 89,
      size: 'M',
      color: 'Black',
      image: 'https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400&q=80',
      qty: 1,
    },
    {
      cartKey: 'graphic-tee-L-White',
      id: 'graphic-tee',
      name: 'Campus Graphic Tee',
      price: 35,
      size: 'L',
      color: 'White',
      image: 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400&q=80',
      qty: 2,
    },
    {
      cartKey: 'wide-cargo-M-Olive',
      id: 'wide-cargo',
      name: 'Wide Leg Cargo',
      price: 65,
      size: 'M',
      color: 'Olive',
      image: 'https://images.unsplash.com/photo-1594938298603-c8148c4b4d7a?w=400&q=80',
      qty: 1,
    },
  ]);

  const cartCount = items.reduce((sum, i) => sum + i.qty, 0);

  const addItem = (item) => {
    const cartKey = `${item.id}-${item.size}-${item.color}`;
    dispatch({ type: 'ADD_ITEM', item: { ...item, cartKey } });
  };

  const removeItem = (cartKey) => dispatch({ type: 'REMOVE_ITEM', cartKey });

  const updateQty = (cartKey, qty) => dispatch({ type: 'UPDATE_QTY', cartKey, qty });

  return (
    <CartContext.Provider value={{ items, cartCount, addItem, removeItem, updateQty }}>
      {children}
    </CartContext.Provider>
  );
}

export const useCart = () => {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
};
