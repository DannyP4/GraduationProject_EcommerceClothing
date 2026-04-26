import { useNavigate } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import { useCart } from '../context/CartContext';
import { products } from '../data/products';

export default function CartPage() {
  const { items, removeItem, updateQty } = useCart();
  const navigate = useNavigate();

  const subtotal = items.reduce((sum, i) => sum + i.price * i.qty, 0);
  const shipping = subtotal >= 75 ? 0 : 8.99;
  const tax = subtotal * 0.08;
  const total = subtotal + shipping + tax;

  const suggestions = products.slice(4, 8);

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

        {items.length === 0 ? (
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
              {/* Header */}
              <div className="hidden md:grid grid-cols-[auto_1fr_auto_auto_auto] gap-6 items-center pb-3 border-b border-black/15">
                {['', 'Product', 'Size / Color', 'Qty', 'Total'].map((h) => (
                  <span key={h} className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">{h}</span>
                ))}
              </div>

              {items.map((item) => (
                <div
                  key={item.cartKey}
                  className="bg-white p-4 md:p-6 flex flex-col md:grid md:grid-cols-[80px_1fr_auto_auto_auto] gap-4 items-start md:items-center"
                >
                  {/* Image */}
                  <div
                    className="w-20 h-24 overflow-hidden cursor-pointer flex-shrink-0"
                    onClick={() => navigate(`/product/${item.id}`)}
                  >
                    <img src={item.image} alt={item.name} className="w-full h-full object-cover" />
                  </div>

                  {/* Info */}
                  <div>
                    <h3
                      className="font-bold text-sm uppercase tracking-wider cursor-pointer hover:text-[#E83354] transition-colors mb-1"
                      onClick={() => navigate(`/product/${item.id}`)}
                    >
                      {item.name}
                    </h3>
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-[10px] font-bold tracking-wider bg-black/8 px-2 py-0.5 uppercase">
                        {item.size}
                      </span>
                      <span className="text-[10px] font-bold tracking-wider bg-black/8 px-2 py-0.5 uppercase">
                        {item.color}
                      </span>
                      {/* VTO badge */}
                      <button
                        onClick={() => navigate('/try-on')}
                        className="text-[9px] font-bold tracking-wider border border-[#E83354] text-[#E83354] px-2 py-0.5 uppercase hover:bg-[#E83354] hover:text-white transition-all"
                      >
                        Try On
                      </button>
                    </div>
                    <p className="text-[11px] text-black/50 mt-1">${item.price} each</p>
                  </div>

                  {/* Size/Color placeholder for grid alignment */}
                  <div className="hidden md:block" />

                  {/* Qty */}
                  <div className="flex items-center border border-black/15">
                    <button
                      onClick={() => updateQty(item.cartKey, item.qty - 1)}
                      className="w-9 h-9 text-lg font-bold flex items-center justify-center hover:bg-black hover:text-white transition-all"
                    >
                      −
                    </button>
                    <span className="w-10 text-center text-sm font-bold">{item.qty}</span>
                    <button
                      onClick={() => updateQty(item.cartKey, item.qty + 1)}
                      className="w-9 h-9 text-lg font-bold flex items-center justify-center hover:bg-black hover:text-white transition-all"
                    >
                      +
                    </button>
                  </div>

                  {/* Total + remove */}
                  <div className="flex flex-col items-end gap-2">
                    <span className="font-['Anton'] text-xl">${(item.price * item.qty).toFixed(2)}</span>
                    <button
                      onClick={() => removeItem(item.cartKey)}
                      className="text-[10px] font-bold tracking-wider uppercase text-black/30 hover:text-[#E83354] transition-colors"
                    >
                      Remove
                    </button>
                  </div>
                </div>
              ))}
            </div>

            {/* CHECKOUT SUMMARY */}
            <aside className="w-72 flex-shrink-0 sticky top-20">
              <div className="bg-[#0A0A0A] text-white p-6">
                <h2 className="font-['Anton'] text-2xl uppercase tracking-wider mb-6">Order Summary</h2>

                <div className="space-y-3 mb-6 text-sm">
                  <div className="flex justify-between">
                    <span className="text-white/60">Subtotal</span>
                    <span>${subtotal.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-white/60">Shipping</span>
                    <span>{shipping === 0 ? <span className="text-green-400">FREE</span> : `$${shipping.toFixed(2)}`}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-white/60">Tax (8%)</span>
                    <span>${tax.toFixed(2)}</span>
                  </div>
                  {shipping > 0 && (
                    <p className="text-[10px] text-white/40 bg-white/5 p-2 mt-1">
                      Add ${(75 - subtotal).toFixed(2)} more for free shipping
                    </p>
                  )}
                </div>

                <div className="border-t border-white/15 pt-4 mb-6">
                  <div className="flex justify-between items-center">
                    <span className="text-[11px] font-bold tracking-[0.1em] uppercase text-white/60">Total</span>
                    <span className="font-['Anton'] text-3xl">${total.toFixed(2)}</span>
                  </div>
                </div>

                {/* Promo code */}
                <div className="flex mb-4">
                  <input
                    type="text"
                    placeholder="Promo code"
                    className="flex-1 bg-white/10 border border-white/20 text-white text-sm px-3 py-2.5 focus:outline-none focus:border-white/50 placeholder:text-white/30"
                  />
                  <button className="bg-white/15 text-white text-[10px] font-bold tracking-wider px-4 hover:bg-white/25 transition-colors">
                    APPLY
                  </button>
                </div>

                <button className="w-full bg-[#E83354] text-white text-[12px] font-bold tracking-[0.15em] uppercase py-4 hover:bg-[#c82244] transition-colors mb-3">
                  Place Order
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

        {/* You May Also Like */}
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
