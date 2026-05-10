import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import ConfirmDialog from './ConfirmDialog';

export default function NavbarGlass() {
  const { cartCount } = useCart();
  const { user, status, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <nav className="sticky top-0 z-50 backdrop-blur-md bg-white/80 border-b border-black/10">
      <div className="max-w-[1440px] mx-auto px-6 flex items-center justify-between h-14">
        <Link
          to="/"
          className="font-['Anton'] text-2xl tracking-widest text-black hover:text-[#E83354] transition-colors"
        >
          UNIFORM
        </Link>

        <div className="hidden md:flex items-center gap-8">
          {['Shop', 'Collections', 'Lookbook', 'About'].map((item) => (
            <Link
              key={item}
              to={item === 'Shop' ? '/shop' : '#'}
              className="text-[11px] font-bold tracking-[0.12em] uppercase text-black/70 hover:text-black transition-colors"
            >
              {item}
            </Link>
          ))}
        </div>

        <div className="flex items-center gap-4">
          <button className="text-black/60 hover:text-black transition-colors" aria-label="Search">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
            </svg>
          </button>

          {status === 'authenticated' ? (
            <UserMenu user={user} onLogout={async () => { await logout(); navigate('/'); }} />
          ) : status === 'loading' ? (
            <span className="hidden sm:block w-12 h-3 bg-black/10 rounded animate-pulse" />
          ) : (
            <Link
              to="/login"
              className="text-[11px] font-bold tracking-[0.1em] uppercase text-black/70 hover:text-black transition-colors hidden sm:block"
            >
              Login
            </Link>
          )}

          <Link to="/cart" className="relative text-black/70 hover:text-black transition-colors" aria-label="Cart">
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
        </div>
      </div>
    </nav>
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
        className="flex items-center gap-2 group"
        aria-haspopup="true"
        aria-expanded={open}
      >
        <span className="w-8 h-8 rounded-full bg-black text-white text-xs font-bold flex items-center justify-center group-hover:bg-[#E83354] transition-colors">
          {initial}
        </span>
        <span
          className="text-[11px] font-bold tracking-[0.1em] uppercase text-black/70 max-w-[120px] truncate group-hover:text-black"
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
          className="absolute right-0 top-full mt-2 w-60 bg-white border border-black/10 shadow-lg py-1"
          role="menu"
        >
          <div className="px-4 py-3 border-b border-black/5">
            <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">Signed in as</p>
            <p className="text-sm font-bold truncate" title={user?.email}>{user?.fullName || user?.email}</p>
            {user?.fullName && <p className="text-[11px] text-black/50 truncate">{user.email}</p>}
          </div>

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
