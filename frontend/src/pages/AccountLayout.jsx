import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import AnnouncementBar from '../components/AnnouncementBar';
import NavbarGlass from '../components/NavbarGlass';
import FooterFull from '../components/FooterFull';
import ConfirmDialog from '../components/ConfirmDialog';
import EmailVerificationBanner from '../components/EmailVerificationBanner';
import { useAuth } from '../context/AuthContext';

const NAV = [
  { to: '/account/profile', label: 'Profile' },
  { to: '/account/orders', label: 'Orders' },
  { to: '/account/addresses', label: 'Addresses' },
];

export default function AccountLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const doLogout = async () => {
    setConfirmOpen(false);
    await logout();
    navigate('/');
  };

  return (
    <div className="min-h-screen bg-[#E8E8E8]">
      <AnnouncementBar />
      <NavbarGlass />

      <div className="max-w-[1440px] mx-auto px-6 py-10">
        <div className="mb-8">
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-1">
            {user?.email}
          </p>
          <h1 className="font-['Anton'] text-5xl md:text-6xl uppercase tracking-tight">My Account</h1>
        </div>

        <EmailVerificationBanner />

        <div className="grid grid-cols-1 md:grid-cols-[220px_minmax(0,1fr)] gap-8 items-start">
          <aside className="bg-white p-4">
            <nav className="flex md:flex-col gap-1">
              {NAV.map((n) => (
                <NavLink
                  key={n.to}
                  to={n.to}
                  className={({ isActive }) =>
                    `block px-3 py-2 text-[11px] font-bold tracking-[0.15em] uppercase transition-colors ${
                      isActive ? 'bg-black text-white' : 'text-black/60 hover:bg-black/5 hover:text-black'
                    }`
                  }
                >
                  {n.label}
                </NavLink>
              ))}
            </nav>
            <button
              onClick={() => setConfirmOpen(true)}
              className="mt-3 md:mt-6 w-full px-3 py-2 text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 text-black/60 hover:border-[#E83354] hover:text-[#E83354] transition-colors"
            >
              Sign Out
            </button>
          </aside>

          <main className="bg-white p-6 md:p-8 min-w-0">
            <Outlet />
          </main>
        </div>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title="Sign out?"
        message="You will need to sign in again to access your account, orders and saved addresses."
        confirmLabel="Sign Out"
        cancelLabel="Stay Signed In"
        tone="danger"
        onCancel={() => setConfirmOpen(false)}
        onConfirm={doLogout}
      />

      <FooterFull />
    </div>
  );
}
