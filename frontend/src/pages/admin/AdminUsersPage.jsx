import { Link } from 'react-router-dom';

export default function AdminUsersPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Users</h1>
        <p className="text-xs text-black/50 mt-1">Customer accounts — ban, unban, soft-delete.</p>
      </div>

      <div className="border border-dashed border-black/20 bg-white px-6 py-20 text-center">
        <span className="inline-block text-[10px] font-bold tracking-[0.25em] uppercase bg-black text-white px-3 py-1 mb-4">
          Coming in Phase 4e
        </span>
        <p className="font-['Anton'] text-3xl uppercase tracking-tight mb-2">User management</p>
        <p className="text-sm text-black/50 max-w-md mx-auto mb-6">
          List + filter by status (ACTIVE / SUSPENDED / DELETED), view detail
          (orders placed, addresses, last login), and toggle status via
          PATCH on <code className="text-xs">users.status</code>.
          Schema is already in V1 — no migration needed.
        </p>
        <Link
          to="/admin"
          className="inline-block text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
        >
          ← Back to Dashboard
        </Link>
      </div>
    </div>
  );
}
