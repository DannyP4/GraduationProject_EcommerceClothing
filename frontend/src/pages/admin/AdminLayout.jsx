import { useState } from 'react';
import { Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import AdminSidebar from '../../components/admin/AdminSidebar';
import AdminTopBar from '../../components/admin/AdminTopBar';
import ConfirmDialog from '../../components/ConfirmDialog';

export default function AdminLayout() {
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
      <AdminTopBar user={user} onSignOutClick={() => setConfirmOpen(true)} />

      <div className="max-w-[1440px] mx-auto px-6 py-8">
        <div className="grid grid-cols-1 md:grid-cols-[220px_1fr] gap-6 items-start">
          <AdminSidebar />
          <main>
            <Outlet />
          </main>
        </div>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title="Sign out?"
        message="You will need to sign in again to access the admin dashboard."
        confirmLabel="Sign Out"
        cancelLabel="Stay Signed In"
        tone="danger"
        onCancel={() => setConfirmOpen(false)}
        onConfirm={doLogout}
      />
    </div>
  );
}
