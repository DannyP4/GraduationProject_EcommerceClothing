import { Link } from 'react-router-dom';
import AdminNotificationBell from './AdminNotificationBell';
import AdminUserMenu from './AdminUserMenu';

export default function AdminTopBar({ user, onSignOutClick }) {
  return (
    <header className="sticky top-0 z-40 backdrop-blur-md bg-white/85 border-b border-black/10">
      <div className="max-w-[1440px] mx-auto px-6 flex items-center justify-between h-14">
        <div className="flex items-center gap-3">
          <Link
            to="/"
            className="font-['Anton'] text-2xl tracking-widest text-black hover:text-[#E83354] transition-colors"
          >
            VESTA
          </Link>
          <span className="text-[10px] font-bold tracking-[0.2em] uppercase bg-black text-white px-2 py-1">
            Admin
          </span>
        </div>

        <div className="flex items-center gap-4">
          <AdminNotificationBell />
          <AdminUserMenu user={user} onSignOutClick={onSignOutClick} />
        </div>
      </div>
    </header>
  );
}
