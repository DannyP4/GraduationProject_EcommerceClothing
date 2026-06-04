import { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import ConfirmDialog from '../../components/ConfirmDialog';
import { useToast } from '../../components/Toast';
import { useAuth } from '../../context/AuthContext';
import * as userSvc from '../../services/adminUserService';
import useScrollRestore from '../../lib/useScrollRestore';
import AdminPagination from '../../components/admin/AdminPagination';

const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'SUSPENDED', label: 'Suspended' },
  { value: 'DELETED', label: 'Deleted' },
  { value: '', label: 'All' },
];
const SORT_OPTIONS = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'createdAt,asc', label: 'Oldest' },
  { value: 'lastLoginAt,desc', label: 'Last login (recent)' },
];
const SEARCH_DEBOUNCE_MS = 400;

export default function AdminUsersPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const { user: currentUser } = useAuth();

  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [searchInput, setSearchInput] = useState(() => searchParams.get('q') ?? '');
  const [search, setSearch] = useState(() => searchParams.get('q') ?? '');
  const [status, setStatus] = useState(() => searchParams.get('status') ?? 'ACTIVE');
  const [sort, setSort] = useState(() => searchParams.get('sort') ?? 'createdAt,desc');
  const [pageIndex, setPageIndex] = useState(() => Math.max(0, Math.floor(Number(searchParams.get('page')) || 1) - 1));
  const pageSize = 20;

  const [confirmSuspend, setConfirmSuspend] = useState(null);
  const [confirmActivate, setConfirmActivate] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(null);

  useScrollRestore(!loading);

  useEffect(() => {
    const id = setTimeout(() => {
      const trimmed = searchInput.trim();
      if (trimmed !== search) {
        setSearch(trimmed);
        setPageIndex(0);
        if (trimmed && status !== '') setStatus('');
      }
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(id);
  }, [searchInput, search, status]);

  useEffect(() => {
    const next = new URLSearchParams();
    if (pageIndex > 0) next.set('page', String(pageIndex + 1));
    if (status !== 'ACTIVE') next.set('status', status);
    if (search) next.set('q', search);
    if (sort !== 'createdAt,desc') next.set('sort', sort);
    if (next.toString() !== searchParams.toString()) {
      setSearchParams(next, { replace: true });
    }
  }, [pageIndex, status, search, sort, searchParams, setSearchParams]);

  const filtersDirty = !!(searchInput || status !== 'ACTIVE' || sort !== 'createdAt,desc');

  const filterParams = useMemo(() => ({
    page: pageIndex,
    size: pageSize,
    sort,
    search: search || undefined,
    status: status || undefined,
  }), [search, status, sort, pageIndex]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await userSvc.listUsers(filterParams);
      setPage(data);
    } catch (err) {
      setError(err.message || 'Could not load users.');
    } finally {
      setLoading(false);
    }
  }, [filterParams]);

  useEffect(() => { load(); }, [load]);

  const clearFilters = () => {
    setSearchInput('');
    setSearch('');
    setStatus('ACTIVE');
    setSort('createdAt,desc');
    setPageIndex(0);
  };

  const handleSuspend = async () => {
    if (!confirmSuspend) return;
    try {
      await userSvc.suspendUser(confirmSuspend.id);
      const name = confirmSuspend.fullName || confirmSuspend.email;
      setConfirmSuspend(null);
      toast.success(`Suspended ${name}`);
      await load();
    } catch (err) {
      toast.error(err.message || 'Suspend failed');
    }
  };

  const handleActivate = async () => {
    if (!confirmActivate) return;
    try {
      await userSvc.activateUser(confirmActivate.id);
      const name = confirmActivate.fullName || confirmActivate.email;
      setConfirmActivate(null);
      toast.success(`Activated ${name}`);
      await load();
    } catch (err) {
      toast.error(err.message || 'Activate failed');
    }
  };

  const handleDelete = async () => {
    if (!confirmDelete) return;
    try {
      await userSvc.softDeleteUser(confirmDelete.id);
      const name = confirmDelete.fullName || confirmDelete.email;
      setConfirmDelete(null);
      toast.success(`Soft-deleted ${name}`);
      await load();
    } catch (err) {
      toast.error(err.message || 'Delete failed');
    }
  };

  const content = page?.content ?? [];
  const totalPages = page?.totalPages ?? 0;
  const totalElements = page?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Users</h1>
        <p className="text-sm text-black/55 mt-1 max-w-md">
          Customer accounts — suspend, reactivate, or soft-delete. Admins cannot be ban'd from this view.
        </p>
      </div>

      <div className="bg-white border border-black/10 p-3">
        <div className="flex flex-wrap items-stretch gap-2">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search email or name..."
            className="flex-1 min-w-[180px] border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
          />
          <select
            value={status}
            onChange={(e) => { setStatus(e.target.value); setPageIndex(0); }}
            className="border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white min-w-[130px]"
          >
            {STATUS_OPTIONS.map((s) => <option key={s.value || 'all'} value={s.value}>{s.label}</option>)}
          </select>
          <select
            value={sort}
            onChange={(e) => { setSort(e.target.value); setPageIndex(0); }}
            className="border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white min-w-[170px]"
          >
            {SORT_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
          </select>
          <button
            type="button"
            onClick={clearFilters}
            disabled={!filtersDirty}
            className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-4 py-2 hover:border-black hover:bg-black hover:text-white transition-colors disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-black/40"
          >
            Clear
          </button>
        </div>
      </div>

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs">{error}</div>
      )}

      <div className="bg-white border border-black/10 overflow-hidden">
        <div className="hidden md:grid grid-cols-[2fr_1.4fr_0.7fr_0.7fr_1fr_0.5fr_0.7fr] gap-3 px-4 py-3 text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 border-b border-black/10 bg-black/[0.02]">
          <span>Email</span>
          <span>Name</span>
          <span>Role</span>
          <span>Status</span>
          <span>Last login</span>
          <span>Orders</span>
          <span className="text-right">Actions</span>
        </div>

        {loading ? (
          <div className="px-6 py-10 text-center text-sm text-black/40">Loading...</div>
        ) : content.length === 0 ? (
          <div className="px-6 py-16 text-center text-sm text-black/50">No users match these filters.</div>
        ) : (
          <ul className="divide-y divide-black/5">
            {content.map((u) => (
              <UserRow
                key={u.id}
                user={u}
                isSelf={currentUser?.email === u.email}
                onView={() => navigate(`/admin/users/${u.id}`, { state: { backTo: `/admin/users${location.search}` } })}
                onSuspend={() => setConfirmSuspend(u)}
                onActivate={() => setConfirmActivate(u)}
                onDelete={() => setConfirmDelete(u)}
              />
            ))}
          </ul>
        )}
      </div>

      <AdminPagination
        page={pageIndex}
        totalPages={totalPages}
        totalElements={totalElements}
        onChange={setPageIndex}
      />

      <ConfirmDialog
        open={!!confirmSuspend}
        title="Suspend user?"
        message={`"${confirmSuspend?.email}" will be blocked from logging in until reactivated. Their existing JWT will stop working on the next request.`}
        confirmLabel="Suspend"
        cancelLabel="Cancel"
        tone="danger"
        onCancel={() => setConfirmSuspend(null)}
        onConfirm={handleSuspend}
      />
      <ConfirmDialog
        open={!!confirmActivate}
        title="Reactivate user?"
        message={`"${confirmActivate?.email}" will be able to log in again.`}
        confirmLabel="Reactivate"
        cancelLabel="Cancel"
        onCancel={() => setConfirmActivate(null)}
        onConfirm={handleActivate}
      />
      <ConfirmDialog
        open={!!confirmDelete}
        title="Soft-delete user?"
        message={`"${confirmDelete?.email}" will be marked DELETED. Their order history and addresses stay in the database for audit. They cannot log in.`}
        confirmLabel="Soft-delete"
        cancelLabel="Cancel"
        tone="danger"
        onCancel={() => setConfirmDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
}

function UserRow({ user, isSelf, onView, onSuspend, onActivate, onDelete }) {
  const isAdmin = user.roleName === 'admin';
  const isDeleted = user.status === 'DELETED';
  const isSuspended = user.status === 'SUSPENDED';
  const lockedByRule = isAdmin || isSelf;

  return (
    <li className="grid grid-cols-2 md:grid-cols-[2fr_1.4fr_0.7fr_0.7fr_1fr_0.5fr_0.7fr] gap-3 px-4 py-3 items-center text-sm">
      <div className="min-w-0">
        <button
          type="button"
          onClick={onView}
          className="font-bold truncate text-left hover:underline hover:text-[#E83354] transition-colors block w-full"
        >
          {user.email}
        </button>
        {isSelf && <p className="text-[10px] text-black/40 italic">(you)</p>}
      </div>
      <div className="hidden md:block text-xs text-black/70 truncate">{user.fullName}</div>
      <div className="hidden md:block">
        <RoleBadge role={user.roleName} />
      </div>
      <div className="hidden md:block">
        <StatusBadge status={user.status} />
      </div>
      <div className="hidden md:block text-xs text-black/50">{formatDateShort(user.lastLoginAt)}</div>
      <div className="hidden md:block text-xs">{user.ordersCount ?? 0}</div>
      <div className="flex justify-end gap-1.5">
        {lockedByRule ? (
          <span className="text-[10px] tracking-[0.15em] uppercase text-black/20">Locked</span>
        ) : (
          <>
            {isSuspended && (
              <IconButton title="Reactivate user" onClick={onActivate} variant="success">
                <CheckIcon />
              </IconButton>
            )}
            {isDeleted && (
              <IconButton title="Restore user" onClick={onActivate} variant="success">
                <RefreshIcon />
              </IconButton>
            )}
            {!isSuspended && !isDeleted && (
              <IconButton title="Suspend user" onClick={onSuspend} variant="warning">
                <LockIcon />
              </IconButton>
            )}
            {!isDeleted && (
              <IconButton title="Soft-delete user" onClick={onDelete} variant="danger">
                <TrashIcon />
              </IconButton>
            )}
          </>
        )}
      </div>
    </li>
  );
}

function RoleBadge({ role }) {
  if (role === 'admin') {
    return <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-black text-white">Admin</span>;
  }
  return <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/60">{role}</span>;
}

function StatusBadge({ status }) {
  if (status === 'ACTIVE') {
    return <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-emerald-600 text-white">Active</span>;
  }
  if (status === 'SUSPENDED') {
    return <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-amber-500 text-white">Suspended</span>;
  }
  return <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-black text-white">Deleted</span>;
}

function IconButton({ title, onClick, variant, children }) {
  const palette = {
    neutral: 'border-black/15 text-black/70 hover:bg-black hover:text-white hover:border-black',
    warning: 'border-amber-600/40 text-amber-700 hover:bg-amber-600 hover:text-white hover:border-amber-600',
    success: 'border-emerald-600/40 text-emerald-700 hover:bg-emerald-600 hover:text-white hover:border-emerald-600',
    danger: 'border-[#E83354]/30 text-[#E83354] hover:bg-[#E83354] hover:text-white hover:border-[#E83354]',
  }[variant];
  return (
    <button
      type="button"
      onClick={onClick}
      title={title}
      aria-label={title}
      className={`w-8 h-8 inline-flex items-center justify-center border transition-colors ${palette}`}
    >
      {children}
    </button>
  );
}

function LockIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  );
}
function CheckIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}
function RefreshIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="23 4 23 10 17 10" />
      <polyline points="1 20 1 14 7 14" />
      <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
    </svg>
  );
}
function TrashIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
    </svg>
  );
}

function formatDateShort(iso) {
  if (!iso) return '-';
  try {
    return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: '2-digit' });
  } catch {
    return iso;
  }
}
