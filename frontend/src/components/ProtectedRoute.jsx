import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children }) {
  const { status } = useAuth();
  const location = useLocation();

  // 'loading' = token present, /auth/me in flight — avoid flashing the login redirect.
  if (status === 'loading') {
    return (
      <div className="min-h-screen flex items-center justify-center text-black/40 text-xs uppercase tracking-widest">
        Loading…
      </div>
    );
  }

  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return children;
}
