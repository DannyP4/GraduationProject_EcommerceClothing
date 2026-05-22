import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';

const MENU_ITEMS = [
  { to: '/account/profile',   label: 'My Profile',    icon: IconUser },
  { to: '/account/orders',    label: 'Order History', icon: IconBox },
  { to: '/account/addresses', label: 'Addresses',     icon: IconMap },
];

export default function AdminUserMenu({ user, onSignOutClick }) {
  const [open, setOpen] = useState(false);
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
    if (closeTimerRef.current) { clearTimeout(closeTimerRef.current); closeTimerRef.current = null; }
  };
  const scheduleClose = () => {
    cancelClose();
    closeTimerRef.current = setTimeout(() => setOpen(false), 180);
  };

  const displayName = user?.fullName || user?.email || 'Admin';
  const firstName = displayName.split(' ')[0] || displayName;
  const initial = (user?.fullName?.[0] || user?.email?.[0] || 'A').toUpperCase();

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
        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
          className={`text-black/40 transition-transform ${open ? 'rotate-180' : ''}`}>
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-2 w-60 bg-white border border-black/10 shadow-lg py-1 z-50" role="menu">
          <div className="px-4 py-3 border-b border-black/5">
            <div className="flex items-center gap-2 mb-1">
              <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40">Signed in as</p>
              <span className="text-[9px] font-bold tracking-wider uppercase bg-black text-white px-1.5 py-0.5">
                {user?.role ?? 'admin'}
              </span>
            </div>
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
              onClick={() => { setOpen(false); onSignOutClick?.(); }}
              className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-black/70 hover:bg-[#E83354]/5 hover:text-[#E83354] transition-colors"
              role="menuitem"
            >
              <IconLogout />
              Sign Out
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function IconUser() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
    </svg>
  );
}
function IconBox() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
      <polyline points="3.27 6.96 12 12.01 20.73 6.96" /><line x1="12" y1="22.08" x2="12" y2="12" />
    </svg>
  );
}
function IconMap() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" /><circle cx="12" cy="10" r="3" />
    </svg>
  );
}
function IconLogout() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  );
}
