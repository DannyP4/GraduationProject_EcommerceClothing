import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { getProducts } from '../services/productService';
import ConfirmDialog from './ConfirmDialog';
import LanguageSwitcher from './LanguageSwitcher';

const NAV_LINKS = [
  { label: 'Shop', to: '/shop' },
  { label: 'Collections', to: '#' },
  { label: 'Lookbook', to: '#' },
  { label: 'About', to: '#' },
];

export default function NavbarGlass() {
  const { user, status, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <nav className="sticky top-0 z-50 backdrop-blur-md bg-white/80 border-b border-black/10">
      <div className="max-w-[1440px] mx-auto px-6 flex items-center justify-between h-14">
        <Link
          to="/"
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          className="font-['Anton'] text-3xl tracking-widest text-black hover:text-[#E83354] transition-all hover:-translate-y-0.5 inline-block"
        >
          VESTA
        </Link>

        <div className="hidden md:flex items-center gap-8">
          {NAV_LINKS.map((item) =>
            item.to === '#' ? (
              <span
                key={item.label}
                className="text-[11px] font-bold tracking-[0.12em] uppercase text-black/40 cursor-not-allowed"
                title="Coming soon"
              >
                {item.label}
              </span>
            ) : (
              <NavLink
                key={item.label}
                to={item.to}
                className={({ isActive }) =>
                  `text-[11px] font-bold tracking-[0.12em] uppercase transition-all relative hover:-translate-y-0.5 hover:text-[#E83354] inline-block ${isActive
                    ? 'text-black after:content-[""] after:absolute after:left-0 after:right-0 after:-bottom-1.5 after:h-0.5 after:bg-[#E83354]'
                    : 'text-black/70'
                  }`
                }
              >
                {item.label}
              </NavLink>
            )
          )}
        </div>

        <div className="flex items-center gap-4">
          <NavbarSearch />

          <LanguageSwitcher />

          {status === 'authenticated' && <NotificationBell />}

          {status === 'authenticated' ? (
            <UserMenu user={user} onLogout={async () => { await logout(); navigate('/'); }} />
          ) : status === 'loading' ? (
            <span className="hidden sm:block w-12 h-3 bg-black/10 rounded animate-pulse" />
          ) : (
            <Link
              to="/login"
              className="text-[11px] font-bold tracking-[0.1em] uppercase text-black/70 hover:text-[#E83354] transition-colors hidden sm:block"
            >
              Login
            </Link>
          )}

          <CartHover />
        </div>
      </div>
    </nav>
  );
}

function NavbarSearch() {
  const [expanded, setExpanded] = useState(false);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const wrapRef = useRef(null);
  const inputRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (!expanded) return;
    const t = setTimeout(() => inputRef.current?.focus(), 50);
    const onDocClick = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) {
        setExpanded(false);
      }
    };
    const onKey = (e) => { if (e.key === 'Escape') { setExpanded(false); setQuery(''); setResults([]); } };
    document.addEventListener('mousedown', onDocClick);
    document.addEventListener('keydown', onKey);
    return () => {
      clearTimeout(t);
      document.removeEventListener('mousedown', onDocClick);
      document.removeEventListener('keydown', onKey);
    };
  }, [expanded]);

  // 250ms delay
  useEffect(() => {
    const q = query.trim();
    if (!expanded || q.length < 2) {
      setResults([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    let cancelled = false;
    const id = setTimeout(async () => {
      try {
        const data = await getProducts({ search: q, size: 8, page: 0 });
        if (!cancelled) setResults(data?.content ?? []);
      } catch {
        if (!cancelled) setResults([]);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }, 250);
    return () => { cancelled = true; clearTimeout(id); };
  }, [query, expanded]);

  const submit = (e) => {
    e?.preventDefault?.();
    const q = query.trim();
    if (!q) {
      if (!expanded) setExpanded(true);
      return;
    }
    navigate(`/shop?q=${encodeURIComponent(q)}`);
    setExpanded(false);
    setQuery('');
    setResults([]);
  };

  const pickProduct = (p) => {
    navigate(`/product/${p.slug || p.id}`);
    setExpanded(false);
    setQuery('');
    setResults([]);
  };

  const trimmed = query.trim();
  const showDropdown = expanded && trimmed.length >= 2;

  return (
    <form
      ref={wrapRef}
      onSubmit={submit}
      className="hidden sm:flex items-center relative"
    >
      <div
        className={`flex items-center transition-all duration-300 overflow-hidden ${expanded ? 'w-56 px-3 bg-white/60 border border-black/15' : 'w-9 px-0'
          }`}
      >
        <button
          type="button"
          onClick={() => (expanded ? submit() : setExpanded(true))}
          className="text-black/60 hover:text-[#E83354] transition-all hover:-translate-y-0.5 flex-shrink-0 w-9 h-9 flex items-center justify-center"
          aria-label={expanded ? 'Submit search' : 'Search'}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
          </svg>
        </button>
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search products..."
          className={`bg-transparent text-sm focus:outline-none placeholder:text-black/30 transition-all ${expanded ? 'w-full opacity-100 ml-1' : 'w-0 opacity-0 pointer-events-none'
            }`}
        />
        {expanded && query && (
          <button
            type="button"
            onClick={() => { setQuery(''); setResults([]); inputRef.current?.focus(); }}
            className="text-black/30 hover:text-black flex-shrink-0"
            aria-label="Clear"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        )}
      </div>

      {showDropdown && (
        <div className="absolute right-0 top-full mt-2 w-80 bg-white border border-black/10 shadow-xl z-50 max-h-[480px] overflow-y-auto">
          {loading ? (
            <div className="px-4 py-6 text-center text-[11px] tracking-wider uppercase text-black/40">Searching…</div>
          ) : results.length === 0 ? (
            <div className="px-4 py-8 text-center">
              <p className="text-sm text-black/50 mb-1">No matches for &ldquo;{trimmed}&rdquo;</p>
              <p className="text-[11px] text-black/40">Try a different keyword</p>
            </div>
          ) : (
            <>
              <p className="px-4 py-2 text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 border-b border-black/5">
                {results.length} {results.length === 1 ? 'match' : 'matches'}
              </p>
              <ul className="divide-y divide-black/5">
                {results.map((p) => (
                  <li key={p.id}>
                    <button
                      type="button"
                      onClick={() => pickProduct(p)}
                      className="w-full flex gap-3 px-4 py-2.5 hover:bg-black/5 transition-colors text-left"
                    >
                      <div className="w-10 h-12 flex-shrink-0 bg-black/5 overflow-hidden">
                        {p.primaryImageUrl && (
                          <img src={p.primaryImageUrl} alt={p.name} className="w-full h-full object-cover" />
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-bold uppercase tracking-wider truncate">{p.name}</p>
                        {p.categoryName && (
                          <p className="text-[10px] text-black/40 mt-0.5 truncate">{p.categoryName}</p>
                        )}
                      </div>
                      <span className="text-xs font-bold whitespace-nowrap text-[#E83354] self-center">
                        {formatPrice(p.basePrice, p.currency)}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
              <button
                type="submit"
                className="w-full px-4 py-3 text-[11px] font-bold tracking-[0.15em] uppercase text-center bg-black text-white hover:bg-[#E83354] transition-colors"
              >
                View all results →
              </button>
            </>
          )}
        </div>
      )}
    </form>
  );
}

// ---------------- Cart hover popover ----------------

function CartHover() {
  const { items, cartCount, subtotal, currency } = useCart();
  const [open, setOpen] = useState(false);
  const wrapRef = useRef(null);
  const closeTimerRef = useRef(null);

  const cancelClose = () => {
    if (closeTimerRef.current) { clearTimeout(closeTimerRef.current); closeTimerRef.current = null; }
  };
  const scheduleClose = () => {
    cancelClose();
    closeTimerRef.current = setTimeout(() => setOpen(false), 200);
  };

  useEffect(() => () => cancelClose(), []);

  const preview = items.slice(0, 4);
  const remaining = Math.max(items.length - preview.length, 0);

  return (
    <div
      ref={wrapRef}
      className="relative"
      onMouseEnter={() => { cancelClose(); setOpen(true); }}
      onMouseLeave={scheduleClose}
    >
      <Link to="/cart" className="relative text-black/70 hover:text-[#E83354] transition-all hover:-translate-y-0.5 block" aria-label="Cart">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
          <line x1="3" y1="6" x2="21" y2="6" />
          <path d="M16 10a4 4 0 0 1-8 0" />
        </svg>
        {cartCount > 0 && (
          <span className="absolute -top-2 -right-2 bg-[#E83354] text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
            {cartCount}
          </span>
        )}
      </Link>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-80 bg-white border border-black/10 shadow-xl py-1 z-50">
          <div className="px-4 py-3 border-b border-black/5 flex items-center justify-between">
            <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">My Cart</p>
            <span className="text-[10px] font-bold tracking-wider uppercase bg-black text-white px-2 py-0.5">
              {cartCount} {cartCount === 1 ? 'item' : 'items'}
            </span>
          </div>

          {items.length === 0 ? (
            <div className="px-4 py-8 text-center">
              <p className="text-sm text-black/50 mb-3">Your cart is empty</p>
              <Link
                to="/shop"
                onClick={() => setOpen(false)}
                className="inline-block text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
              >
                Shop Now
              </Link>
            </div>
          ) : (
            <>
              <ul className="max-h-80 overflow-y-auto divide-y divide-black/5">
                {preview.map((it) => (
                  <li key={it.id ?? `g-${it.variantId}`} className="flex gap-3 px-4 py-2.5">
                    <div className="w-10 h-12 flex-shrink-0 bg-black/5 overflow-hidden">
                      {it.imageUrl && <img src={it.imageUrl} alt={it.productName} className="w-full h-full object-cover" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-bold uppercase tracking-wider truncate">{it.productName ?? 'Product'}</p>
                      <p className="text-[10px] text-black/50">
                        {it.size}{it.color ? ` · ${it.color}` : ''} · ×{it.quantity}
                      </p>
                    </div>
                    <span className="text-xs font-bold whitespace-nowrap">
                      {formatPrice(it.lineTotal, it.currency ?? currency)}
                    </span>
                  </li>
                ))}
              </ul>

              {remaining > 0 && (
                <p className="px-4 py-2 text-[10px] tracking-wider text-black/40 text-center border-t border-black/5">
                  +{remaining} more {remaining === 1 ? 'item' : 'items'} in cart
                </p>
              )}

              <div className="px-4 py-3 border-t border-black/5 flex items-center justify-between">
                <span className="text-[10px] font-bold tracking-wider uppercase text-black/50">Subtotal</span>
                <span className="font-['Anton'] text-lg">{formatPrice(subtotal, currency)}</span>
              </div>

              <div className="px-4 py-3 grid grid-cols-2 gap-2 border-t border-black/5">
                <Link
                  to="/cart"
                  onClick={() => setOpen(false)}
                  className="block text-center text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 py-2 hover:border-black transition-colors"
                >
                  View Cart
                </Link>
                <Link
                  to="/checkout"
                  onClick={() => setOpen(false)}
                  className="block text-center text-[11px] font-bold tracking-[0.15em] uppercase bg-[#E83354] text-white py-2 hover:bg-[#c82244] transition-colors"
                >
                  Checkout
                </Link>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}

function NotificationBell() {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef(null);
  const closeTimerRef = useRef(null);

  const cancelClose = () => {
    if (closeTimerRef.current) { clearTimeout(closeTimerRef.current); closeTimerRef.current = null; }
  };
  const scheduleClose = () => {
    cancelClose();
    closeTimerRef.current = setTimeout(() => setOpen(false), 200);
  };

  useEffect(() => () => cancelClose(), []);

  return (
    <div
      ref={wrapRef}
      className="relative hidden sm:block"
      onMouseEnter={() => { cancelClose(); setOpen(true); }}
      onMouseLeave={scheduleClose}
    >
      <button
        type="button"
        aria-label="Notifications"
        className="text-black/70 hover:text-[#E83354] transition-all hover:-translate-y-0.5 block"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-72 bg-white border border-black/10 shadow-xl z-50">
          <div className="px-4 py-3 border-b border-black/5 flex items-center justify-between">
            <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">Notifications</p>
            <span className="text-[9px] tracking-[0.15em] uppercase bg-black/5 text-black/40 px-2 py-0.5">Coming soon</span>
          </div>
          <div className="px-4 py-8 text-center">
            <p className="text-sm text-black/50">No notifications yet</p>
            <p className="text-[11px] text-black/40 mt-1">Order updates and promos will appear here.</p>
          </div>
        </div>
      )}
    </div>
  );
}

const MENU_ITEMS = [
  { to: '/account/profile', label: 'My Profile', icon: IconUser },
  { to: '/account/orders', label: 'Order History', icon: IconBox },
  { to: '/account/addresses', label: 'Addresses', icon: IconMap },
];

function UserMenu({ user, onLogout }) {
  const [open, setOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const wrapRef = useRef(null);
  const closeTimerRef = useRef(null);

  useEffect(() => {
    const onDocClick = (e) => {
      if (wrapRef.current && !wrapRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener('mousedown', onDocClick);
    return () => document.removeEventListener('mousedown', onDocClick);
  }, []);

  const cancelClose = () => {
    if (closeTimerRef.current) {
      clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
  };
  const scheduleClose = () => {
    cancelClose();
    closeTimerRef.current = setTimeout(() => setOpen(false), 180);
  };

  const displayName = user?.fullName || user?.email || '';
  const firstName = displayName.split(' ')[0] || displayName;
  const initial = (user?.fullName?.[0] || user?.email?.[0] || '?').toUpperCase();

  return (
    <div
      ref={wrapRef}
      className="relative hidden sm:block"
      onMouseEnter={() => { cancelClose(); setOpen(true); }}
      onMouseLeave={scheduleClose}
    >
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex items-center gap-2 group transition-transform hover:-translate-y-0.5"
        aria-haspopup="true"
        aria-expanded={open}
      >
        <span className="w-8 h-8 rounded-full bg-black text-white text-xs font-bold flex items-center justify-center group-hover:bg-[#E83354] transition-colors">
          {initial}
        </span>
        <span
          className="text-[11px] font-bold tracking-[0.1em] uppercase text-black/70 max-w-[120px] truncate group-hover:text-[#E83354]"
          title={displayName}
        >
          {firstName}
        </span>
        <svg
          width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
          className={`text-black/40 transition-transform ${open ? 'rotate-180' : ''}`}
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>

      {open && (
        <div
          className="absolute right-0 top-full mt-2 w-60 bg-white border border-black/10 shadow-lg py-1 z-50"
          role="menu"
        >
          <div className="px-4 py-3 border-b border-black/5">
            <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">Signed in as</p>
            <p className="text-sm font-bold truncate" title={user?.email}>{user?.fullName || user?.email}</p>
            {user?.fullName && <p className="text-[11px] text-black/50 truncate">{user.email}</p>}
          </div>

          {user?.role === 'admin' && (
            <Link
              to="/admin"
              onClick={() => setOpen(false)}
              className="flex items-center gap-3 px-4 py-2.5 text-sm font-bold bg-[#F5C842]/15 text-black hover:bg-[#F5C842]/35 border-l-2 border-[#F5C842] transition-colors"
              role="menuitem"
            >
              <IconShield />
              Admin Dashboard
            </Link>
          )}

          {MENU_ITEMS.map(({ to, label, icon: Icon }) => (
            <Link
              key={to}
              to={to}
              onClick={() => setOpen(false)}
              className="flex items-center gap-3 px-4 py-2.5 text-sm text-black/70 hover:bg-black/5 hover:text-black transition-colors"
              role="menuitem"
            >
              <Icon />
              {label}
            </Link>
          ))}

          <div className="border-t border-black/5 mt-1">
            <button
              type="button"
              onClick={() => { setOpen(false); setConfirmOpen(true); }}
              className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-black/70 hover:bg-[#E83354]/5 hover:text-[#E83354] transition-colors"
              role="menuitem"
            >
              <IconLogout />
              Sign Out
            </button>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={confirmOpen}
        title="Sign out?"
        message="You will need to sign in again to access your account, orders and saved addresses."
        confirmLabel="Sign Out"
        cancelLabel="Stay Signed In"
        tone="danger"
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => { setConfirmOpen(false); onLogout(); }}
      />
    </div>
  );
}

function formatPrice(value, currency) {
  if (value == null) return '';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
}

function IconUser() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
      <circle cx="12" cy="7" r="4" />
    </svg>
  );
}
function IconBox() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
      <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
      <line x1="12" y1="22.08" x2="12" y2="12" />
    </svg>
  );
}
function IconMap() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
      <circle cx="12" cy="10" r="3" />
    </svg>
  );
}
function IconLogout() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  );
}
function IconShield() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
  );
}
