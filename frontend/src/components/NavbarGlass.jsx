import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { getProducts } from '../services/productService';
import ConfirmDialog from './ConfirmDialog';
import LanguageSwitcher from './LanguageSwitcher';
import useAutoHideScrollbar from '../lib/useAutoHideScrollbar';
import {
  getNotifications,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
} from '../services/notificationService';
import { useTranslation } from 'react-i18next';
import { formatPrice } from '../lib/format';
import { colorLabel } from '../lib/labels';

const NAV_LINKS = [
  { key: 'shop', to: '/shop' },
  { key: 'collections', to: '#' },
  { key: 'lookbook', to: '#' },
  { key: 'about', to: '#' },
];

export default function NavbarGlass() {
  const { user, status, logout } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();

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
                key={item.key}
                className="text-[11px] font-bold tracking-[0.12em] uppercase text-black/40 cursor-not-allowed"
                title={t('nav.comingSoon')}
              >
                {t(`nav.${item.key}`)}
              </span>
            ) : (
              <NavLink
                key={item.key}
                to={item.to}
                className={({ isActive }) =>
                  `text-[11px] font-bold tracking-[0.12em] uppercase transition-all relative hover:-translate-y-0.5 hover:text-[#E83354] inline-block ${isActive
                    ? 'text-black after:content-[""] after:absolute after:left-0 after:right-0 after:-bottom-1.5 after:h-0.5 after:bg-[#E83354]'
                    : 'text-black/70'
                  }`
                }
              >
                {t(`nav.${item.key}`)}
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
              {t('nav.login')}
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
  const { t } = useTranslation();

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
          aria-label={expanded ? t('search.submit') : t('search.label')}
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
          placeholder={t('search.placeholder')}
          className={`bg-transparent text-sm focus:outline-none placeholder:text-black/30 transition-all ${expanded ? 'w-full opacity-100 ml-1' : 'w-0 opacity-0 pointer-events-none'
            }`}
        />
        {expanded && query && (
          <button
            type="button"
            onClick={() => { setQuery(''); setResults([]); inputRef.current?.focus(); }}
            className="text-black/30 hover:text-black flex-shrink-0"
            aria-label={t('search.clear')}
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
            <div className="px-4 py-6 text-center text-[11px] tracking-wider uppercase text-black/40">{t('search.searching')}</div>
          ) : results.length === 0 ? (
            <div className="px-4 py-8 text-center">
              <p className="text-sm text-black/50 mb-1">{t('search.noMatches', { query: trimmed })}</p>
              <p className="text-[11px] text-black/40">{t('search.tryDifferent')}</p>
            </div>
          ) : (
            <>
              <p className="px-4 py-2 text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 border-b border-black/5">
                {t('search.matches', { count: results.length })}
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
                {t('search.viewAll')}
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
  const { t } = useTranslation();
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
      <Link to="/cart" className="relative text-black/70 hover:text-[#E83354] transition-all hover:-translate-y-0.5 block" aria-label={t('cart.label')}>
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
            <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">{t('cart.title')}</p>
            <span className="text-[10px] font-bold tracking-wider uppercase bg-black text-white px-2 py-0.5">
              {t('cart.items', { count: cartCount })}
            </span>
          </div>

          {items.length === 0 ? (
            <div className="px-4 py-8 text-center">
              <p className="text-sm text-black/50 mb-3">{t('cart.empty')}</p>
              <Link
                to="/shop"
                onClick={() => setOpen(false)}
                className="inline-block text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
              >
                {t('cart.shopNow')}
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
                        {it.size}{it.color ? ` · ${colorLabel(t, it.color)}` : ''} · ×{it.quantity}
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
                  {t('cart.moreInCart', { count: remaining })}
                </p>
              )}

              <div className="px-4 py-3 border-t border-black/5 flex items-center justify-between">
                <span className="text-[10px] font-bold tracking-wider uppercase text-black/50">{t('cart.subtotal')}</span>
                <span className="font-['Anton'] text-lg">{formatPrice(subtotal, currency)}</span>
              </div>

              <div className="px-4 py-3 grid grid-cols-2 gap-2 border-t border-black/5">
                <Link
                  to="/cart"
                  onClick={() => setOpen(false)}
                  className="block text-center text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 py-2 hover:border-black transition-colors"
                >
                  {t('cart.viewCart')}
                </Link>
                <Link
                  to="/checkout"
                  onClick={() => setOpen(false)}
                  className="block text-center text-[11px] font-bold tracking-[0.15em] uppercase bg-[#E83354] text-white py-2 hover:bg-[#c82244] transition-colors"
                >
                  {t('cart.checkout')}
                </Link>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}

const NOTIF_META = {
  ORDER_PLACED:    { label: 'Placed',    cls: 'text-blue-700    bg-blue-50    border-blue-300',    icon: IconBox },
  ORDER_PAID:      { label: 'Paid',      cls: 'text-emerald-700 bg-emerald-50 border-emerald-300', icon: IconCheck },
  ORDER_SHIPPED:   { label: 'Shipped',   cls: 'text-indigo-700  bg-indigo-50  border-indigo-300',  icon: IconTruck },
  ORDER_DELIVERED: { label: 'Delivered', cls: 'text-emerald-700 bg-emerald-50 border-emerald-300', icon: IconCheck },
  ORDER_CANCELLED: { label: 'Cancelled', cls: 'text-rose-700    bg-rose-50    border-rose-300',    icon: IconCancel },
  ORDER_REFUNDED:  { label: 'Refunded',  cls: 'text-amber-700   bg-amber-50   border-amber-300',   icon: IconRefund },
};

const NOTIF_POLL_MS = 60000;

function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [unread, setUnread] = useState(0);
  const [total, setTotal] = useState(0);
  const { t } = useTranslation();
  const wrapRef = useRef(null);
  const closeTimerRef = useRef(null);

  const refreshUnread = async () => {
    try { setUnread(await getUnreadCount()); } catch { /* keep last on transient error */ }
  };
  const loadList = async () => {
    try {
      const data = await getNotifications();
      setItems(Array.isArray(data?.content) ? data.content : []);
      setTotal(Number(data?.totalElements ?? 0));
    } catch { /* keep last on transient error */ }
  };

  useEffect(() => {
    refreshUnread();
    const timer = setInterval(refreshUnread, NOTIF_POLL_MS);
    return () => clearInterval(timer);
  }, []);

  const cancelClose = () => {
    if (closeTimerRef.current) { clearTimeout(closeTimerRef.current); closeTimerRef.current = null; }
  };
  const scheduleClose = () => {
    cancelClose();
    closeTimerRef.current = setTimeout(() => setOpen(false), 200);
  };
  const openPanel = () => { cancelClose(); setOpen(true); loadList(); };

  useEffect(() => () => cancelClose(), []);

  const onItemClick = (n) => {
    if (!n.read) {
      markNotificationRead(n.id).catch(() => {});
      setItems((prev) => prev.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      setUnread((u) => Math.max(0, u - 1));
    }
    setOpen(false);
  };

  const onMarkAll = async () => {
    try { await markAllNotificationsRead(); } catch { /* ignore */ }
    setItems((prev) => prev.map((x) => ({ ...x, read: true })));
    setUnread(0);
  };

  return (
    <div
      ref={wrapRef}
      className="relative hidden sm:block"
      onMouseEnter={openPanel}
      onMouseLeave={scheduleClose}
    >
      <button
        type="button"
        aria-label={t('notifications.label')}
        className="relative text-black/70 hover:text-[#E83354] transition-all hover:-translate-y-0.5 block"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>
        {unread > 0 && (
          <span className="absolute -top-2 -right-2 bg-[#E83354] text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
            {unread > 9 ? '9+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-80 bg-white border border-black/10 shadow-xl z-50 max-h-[440px] flex flex-col">
          <div className="px-4 py-3 border-b border-black/5 flex items-center justify-between flex-shrink-0">
            <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">{t('notifications.title')}</p>
            <span className="flex items-center gap-1.5">
              {unread > 0 && (
                <span className="text-[10px] font-bold tracking-wider uppercase bg-[#E83354] text-white px-2 py-0.5">
                  {t('notifications.new', { count: unread })}
                </span>
              )}
              <span className="text-[10px] font-bold tracking-wider uppercase bg-black text-white px-2 py-0.5">
                {total}
              </span>
              {unread > 0 && (
                <button
                  type="button"
                  onClick={onMarkAll}
                  className="text-[10px] font-bold tracking-wider uppercase text-[#E83354] hover:underline ml-1"
                >
                  {t('notifications.markAllRead')}
                </button>
              )}
            </span>
          </div>

          {items.length === 0 ? (
            <div className="px-4 py-8 text-center flex-1">
              <p className="text-sm text-black/50">{t('notifications.empty')}</p>
              <p className="text-[11px] text-black/40 mt-1">{t('notifications.emptyHint')}</p>
            </div>
          ) : (
            <NotificationList items={items} onItemClick={onItemClick} />
          )}
        </div>
      )}
    </div>
  );
}

function extractOrderNumber(n) {
  const fromHref = (n.href || '').match(/\/orders\/([^/?#]+)$/);
  if (fromHref) return fromHref[1];
  const fromMsg = (n.message || '').match(/#([A-Za-z0-9-]+)/);
  return fromMsg ? fromMsg[1] : '';
}

function NotificationList({ items, onItemClick }) {
  const { t } = useTranslation();
  const listRef = useAutoHideScrollbar();
  return (
    <ul ref={listRef} className="overflow-y-auto divide-y divide-black/5 flex-1 scrollbar-subtle">
      {items.map((n) => {
        const meta = NOTIF_META[n.type] ?? { label: n.type, cls: 'text-black/60 bg-black/5 border-black/15', icon: IconBox };
        const Icon = meta.icon;
        return (
          <li key={n.id}>
            <Link
              to={n.href || '#'}
              onClick={() => onItemClick(n)}
              className={`flex gap-3 px-4 py-3 hover:bg-black/5 transition-colors ${n.read ? 'opacity-60' : ''}`}
            >
              <span className={`w-7 h-7 flex-shrink-0 border flex items-center justify-center ${meta.cls}`}>
                <Icon />
              </span>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-0.5">
                  <span className={`text-[9px] font-bold tracking-wider uppercase px-1.5 py-0.5 border ${meta.cls}`}>
                    {t(`notifications.type.${n.type}`, { defaultValue: meta.label })}
                  </span>
                  {!n.read && <span className="w-1.5 h-1.5 rounded-full bg-[#E83354]" />}
                </div>
                <p className="text-xs text-black/80 leading-snug">{t(`notifications.body.${n.type}`, { order: extractOrderNumber(n), defaultValue: n.message })}</p>
                {n.createdAt && <p className="text-[10px] text-black/40 mt-1 tracking-wider">{notifTimeAgo(n.createdAt)}</p>}
              </div>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

function notifTimeAgo(iso) {
  if (!iso) return '';
  const diffMin = Math.max(1, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffH = Math.round(diffMin / 60);
  if (diffH < 24) return `${diffH}h ago`;
  return `${Math.round(diffH / 24)}d ago`;
}

function IconCheck() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}
function IconTruck() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <rect x="1" y="3" width="15" height="13" /><polygon points="16 8 20 8 23 11 23 16 16 16 16 8" />
      <circle cx="5.5" cy="18.5" r="2.5" /><circle cx="18.5" cy="18.5" r="2.5" />
    </svg>
  );
}
function IconCancel() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <circle cx="12" cy="12" r="10" /><line x1="15" y1="9" x2="9" y2="15" /><line x1="9" y1="9" x2="15" y2="15" />
    </svg>
  );
}
function IconRefund() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="1 4 1 10 7 10" /><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
    </svg>
  );
}

function IconHeart() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
    </svg>
  );
}

const MENU_ITEMS = [
  { to: '/account/profile', key: 'profile', icon: IconUser },
  { to: '/account/orders', key: 'orders', icon: IconBox },
  { to: '/account/wishlist', key: 'wishlist', icon: IconHeart },
  { to: '/account/addresses', key: 'addresses', icon: IconMap },
];

function UserMenu({ user, onLogout }) {
  const [open, setOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const { t } = useTranslation();
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
            <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">{t('account.signedInAs')}</p>
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
              {t('account.adminDashboard')}
            </Link>
          )}

          {MENU_ITEMS.map(({ to, key, icon: Icon }) => (
            <Link
              key={to}
              to={to}
              onClick={() => setOpen(false)}
              className="flex items-center gap-3 px-4 py-2.5 text-sm text-black/70 hover:bg-black/5 hover:text-black transition-colors"
              role="menuitem"
            >
              <Icon />
              {t(`account.${key}`)}
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
              {t('account.signOut')}
            </button>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={confirmOpen}
        title={t('account.signOutConfirmTitle')}
        message={t('account.signOutConfirmMessage')}
        confirmLabel={t('account.signOut')}
        cancelLabel={t('account.staySignedIn')}
        tone="danger"
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => { setConfirmOpen(false); onLogout(); }}
      />
    </div>
  );
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
