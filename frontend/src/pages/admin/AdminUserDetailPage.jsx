import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import ConfirmDialog from '../../components/ConfirmDialog';
import OrderStatusBadge from '../../components/admin/OrderStatusBadge';
import { useToast } from '../../components/Toast';
import { useAuth } from '../../context/AuthContext';
import { goBack } from '../../lib/historyBack';
import * as userSvc from '../../services/adminUserService';

export default function AdminUserDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const backTo = location.state?.backTo || '/admin/users';
  const toast = useToast();
  const { user: currentUser } = useAuth();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [confirm, setConfirm] = useState(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const d = await userSvc.getUser(id);
      setData(d);
    } catch (e) {
      setError(e.message || 'Could not load user.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { refresh(); }, [refresh]);

  const isSelf = currentUser?.email === data?.email;
  const isAdmin = data?.roleName === 'admin';
  const lockedByRule = isAdmin || isSelf;

  const runAction = async (action) => {
    const choice = confirm;
    setConfirm(null);
    try {
      const updated = await action();
      setData(updated);
      toast.success(`User ${choice?.verb}`);
    } catch (e) {
      toast.error(e.message || `${choice?.verb} failed`);
    }
  };

  const askSuspend = () => setConfirm({
    kind: 'suspend', verb: 'suspended', tone: 'danger',
    title: 'Suspend user?',
    message: `"${data.email}" will be blocked from logging in until reactivated. Their existing JWT will stop working on the next request.`,
    action: () => userSvc.suspendUser(data.id),
  });
  const askActivate = () => setConfirm({
    kind: 'activate', verb: 'activated',
    title: 'Reactivate user?',
    message: `"${data.email}" will be able to log in again.`,
    action: () => userSvc.activateUser(data.id),
  });
  const askDelete = () => setConfirm({
    kind: 'delete', verb: 'soft-deleted', tone: 'danger',
    title: 'Soft-delete user?',
    message: `"${data.email}" will be marked DELETED. Order history and addresses stay for audit. They cannot log in.`,
    action: () => userSvc.softDeleteUser(data.id),
  });

  if (loading && !data) return <p className="text-sm text-black/40">Loading...</p>;
  if (error) {
    return (
      <div className="bg-white border border-black/10 p-8 text-center">
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs">{error}</div>
        <button
          onClick={() => goBack(navigate, location, '/admin/users')}
          className="inline-block mt-4 text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-4 py-2 hover:bg-black hover:text-white transition-colors"
        >
          Back to Users
        </button>
      </div>
    );
  }
  if (!data) return null;

  return (
    <div className="space-y-6">
      <div>
        <button
          type="button"
          onClick={() => goBack(navigate, location, '/admin/users')}
          className="inline-flex items-center gap-2 text-[11px] font-bold tracking-[0.15em] uppercase border border-black px-3 py-2 hover:bg-black hover:text-white transition-colors"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          All Users
        </button>
      </div>

      <div>
        <div className="flex items-center gap-3 flex-wrap">
          <h1 className="font-['Anton'] text-4xl uppercase tracking-tight break-all">{data.email}</h1>
          <StatusBadge status={data.status} />
          {data.roleName === 'admin' && (
            <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-black text-white">Admin</span>
          )}
        </div>
        <p className="text-xs text-black/50 mt-1">Member since {formatDate(data.createdAt)}</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_320px] gap-6 items-start">
        <div className="space-y-5">
          <Section title="Profile">
            <div className="grid grid-cols-2 gap-4 text-xs">
              <Field label="Full name" value={data.fullName} />
              <Field label="Phone" value={data.phone || '-'} />
              <Field label="Locale" value={data.preferredLocale} />
              <Field label="Email verified" value={data.emailVerifiedAt ? formatDate(data.emailVerifiedAt) : '-'} />
              <Field label="Last login" value={data.lastLoginAt ? formatDate(data.lastLoginAt) : 'Never'} />
              <Field label="Total orders" value={data.ordersCount ?? 0} />
            </div>
          </Section>

          <Section title={`Addresses (${data.addresses?.length ?? 0})`}>
            {(!data.addresses || data.addresses.length === 0) ? (
              <p className="text-xs text-black/40">No addresses on file.</p>
            ) : (
              <ul className="space-y-3">
                {data.addresses.map((a) => (
                  <li key={a.id} className="border-l-2 border-black/10 pl-3">
                    <div className="flex items-center gap-2 mb-1">
                      <p className="text-sm font-bold">{a.recipient}</p>
                      {a.isDefault && (
                        <span className="text-[9px] font-bold tracking-[0.15em] uppercase bg-black text-white px-1.5 py-0.5">Default</span>
                      )}
                      {a.label && <span className="text-[10px] text-black/40 uppercase">{a.label}</span>}
                    </div>
                    <p className="text-xs text-black/60">{a.phone}</p>
                    <p className="text-xs text-black/70 leading-relaxed">
                      {a.line1}{a.ward ? `, ${a.ward}` : ''}, {a.district}, {a.city}
                      {a.postalCode ? ` ${a.postalCode}` : ''} · {a.country}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </Section>

          <Section title={`Recent orders (${data.orders?.length ?? 0}${data.ordersCount > (data.orders?.length ?? 0) ? ` of ${data.ordersCount}` : ''})`}>
            {(!data.orders || data.orders.length === 0) ? (
              <p className="text-xs text-black/40">No orders placed yet.</p>
            ) : (
              <ul className="divide-y divide-black/8">
                {data.orders.map((o) => (
                  <li key={o.orderNumber} className="flex items-center gap-3 py-3 text-xs">
                    <Link
                      to={`/admin/orders/${o.orderNumber}`}
                      className="font-mono font-bold hover:text-[#E83354] transition-colors"
                    >
                      {o.orderNumber}
                    </Link>
                    <OrderStatusBadge status={o.status} />
                    <span className="text-black/40">{formatDate(o.placedAt)}</span>
                    <span className="ml-auto font-bold">{formatMoney(o.grandTotal, o.currency)}</span>
                  </li>
                ))}
              </ul>
            )}
          </Section>
        </div>

        <aside className="space-y-4 lg:sticky lg:top-20">
          <div className="bg-white border border-black/10 p-5 space-y-3">
            <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-black/40">Admin Actions</p>

            {lockedByRule && (
              <p className="text-[11px] text-black/40 leading-relaxed">
                {isAdmin && 'Admin accounts cannot be suspended or deleted from this UI.'}
                {!isAdmin && isSelf && 'You cannot ban your own account.'}
              </p>
            )}

            {!lockedByRule && data.status === 'ACTIVE' && (
              <>
                <button
                  onClick={askSuspend}
                  className="w-full text-[11px] font-bold tracking-[0.15em] uppercase py-3 border border-amber-600 text-amber-700 hover:bg-amber-600 hover:text-white transition-colors"
                >
                  Suspend
                </button>
                <button
                  onClick={askDelete}
                  className="w-full text-[11px] font-bold tracking-[0.15em] uppercase py-3 bg-[#E83354] text-white hover:bg-[#c82244] transition-colors"
                >
                  Soft-delete
                </button>
              </>
            )}

            {!lockedByRule && data.status === 'SUSPENDED' && (
              <>
                <button
                  onClick={askActivate}
                  className="w-full text-[11px] font-bold tracking-[0.15em] uppercase py-3 bg-emerald-600 text-white hover:bg-emerald-700 transition-colors"
                >
                  Reactivate
                </button>
                <button
                  onClick={askDelete}
                  className="w-full text-[11px] font-bold tracking-[0.15em] uppercase py-3 border border-[#E83354] text-[#E83354] hover:bg-[#E83354] hover:text-white transition-colors"
                >
                  Soft-delete
                </button>
              </>
            )}

            {!lockedByRule && data.status === 'DELETED' && (
              <button
                onClick={askActivate}
                className="w-full text-[11px] font-bold tracking-[0.15em] uppercase py-3 bg-emerald-600 text-white hover:bg-emerald-700 transition-colors"
              >
                Restore (set ACTIVE)
              </button>
            )}
          </div>

          <div className="bg-[#0A0A0A] text-white p-5">
            <p className="text-[10px] font-bold tracking-[0.2em] uppercase text-white/40 mb-3">Identity</p>
            <div className="space-y-2 text-xs">
              <Row label="User ID" value={`#${data.id}`} />
              <Row label="Role" value={data.roleName} />
              <Row label="Created" value={formatDate(data.createdAt)} />
              <Row label="Updated" value={formatDate(data.updatedAt)} />
            </div>
          </div>
        </aside>
      </div>

      <ConfirmDialog
        open={!!confirm}
        title={confirm?.title}
        message={confirm?.message}
        confirmLabel={confirm?.kind === 'activate' ? 'Reactivate' : confirm?.kind === 'suspend' ? 'Suspend' : 'Soft-delete'}
        cancelLabel="Cancel"
        tone={confirm?.tone}
        onCancel={() => setConfirm(null)}
        onConfirm={() => runAction(confirm.action)}
      />
    </div>
  );
}

function Section({ title, children }) {
  return (
    <section className="border border-black/10 bg-white p-5">
      <h3 className="font-['Anton'] text-lg uppercase tracking-wider mb-4">{title}</h3>
      {children}
    </section>
  );
}

function Field({ label, value }) {
  return (
    <div>
      <p className="text-[10px] font-bold tracking-wider uppercase text-black/40 mb-1">{label}</p>
      <div className="text-xs text-black/80 truncate">{value}</div>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between items-baseline">
      <span className="text-white/60">{label}</span>
      <span className="font-bold">{value}</span>
    </div>
  );
}

function StatusBadge({ status }) {
  const palette = status === 'ACTIVE'
    ? 'bg-emerald-600 text-white'
    : status === 'SUSPENDED'
      ? 'bg-amber-500 text-white'
      : 'bg-black text-white';
  return <span className={`text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 ${palette}`}>{status}</span>;
}

function formatMoney(value, currency) {
  if (value == null) return '-';
  const num = Number(value);
  if (currency === 'USD') return `$${num.toFixed(2)}`;
  return `${num.toLocaleString('vi-VN')} ₫`;
}

function formatDate(iso) {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}
