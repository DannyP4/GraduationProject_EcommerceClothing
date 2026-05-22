import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function AdminForbiddenPage() {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-[#E8E8E8] flex items-center justify-center px-6 py-16">
      <div className="max-w-md w-full bg-white border border-black/10 p-8 text-center">
        <p className="text-[10px] font-bold tracking-[0.3em] uppercase text-[#E83354] mb-3">403 · Forbidden</p>
        <h1 className="font-['Anton'] text-5xl uppercase tracking-tight mb-3">No access</h1>
        <p className="text-sm text-black/60 mb-6">
          The admin dashboard is restricted to staff accounts. Your account
          <span className="font-bold"> {user?.email ?? ''} </span>
          is signed in as
          <span className="font-bold"> {user?.role ?? 'guest'}</span>.
        </p>
        <div className="flex flex-col sm:flex-row gap-3">
          <Link
            to="/"
            className="flex-1 text-[11px] font-bold tracking-[0.15em] uppercase bg-black text-white py-3 hover:bg-[#E83354] transition-colors"
          >
            Back to Store
          </Link>
          <Link
            to="/login"
            state={{ from: '/admin' }}
            className="flex-1 text-[11px] font-bold tracking-[0.15em] uppercase border border-black/20 py-3 hover:border-black transition-colors"
          >
            Sign in as admin
          </Link>
        </div>
      </div>
    </div>
  );
}
