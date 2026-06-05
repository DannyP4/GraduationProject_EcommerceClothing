import { useCallback, useEffect, useState } from 'react';
import ConfirmDialog from '../../components/ConfirmDialog';
import CouponFormModal from '../../components/admin/CouponFormModal';
import * as svc from '../../services/adminCouponService';
import * as catSvc from '../../services/adminCategoryService';

function formatPrice(value) {
  if (value == null) return '';
  const num = Number(value);
  if (Number.isNaN(num)) return String(value);
  return new Intl.NumberFormat('vi-VN').format(num) + ' ₫';
}

function formatValue(c) {
  return c.type === 'PERCENT' ? `${Number(c.value)}%` : formatPrice(c.value);
}

const SCOPE_LABEL = { ALL: 'Whole order', CATEGORY: 'Categories', PRODUCT: 'Products' };

function formatWindow(c) {
  const fmt = (iso) => new Date(iso).toLocaleDateString('vi-VN');
  if (!c.startsAt && !c.endsAt) return 'Always on';
  return `${c.startsAt ? fmt(c.startsAt) : '—'} → ${c.endsAt ? fmt(c.endsAt) : '—'}`;
}

export default function AdminCouponsPage() {
  const [coupons, setCoupons] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState('create');
  const [modalInitial, setModalInitial] = useState(null);

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pendingDelete, setPendingDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await svc.listCoupons({ size: 100 });
      setCoupons(page?.content ?? []);
    } catch (err) {
      setError(err.message || 'Could not load coupons.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    let cancelled = false;
    catSvc.listCategories()
      .then((c) => { if (!cancelled) setCategories(c ?? []); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const handleCreate = () => {
    setModalMode('create');
    setModalInitial(null);
    setModalOpen(true);
  };

  const handleEdit = async (coupon) => {
    setModalMode('edit');
    // list rows are summaries (no scope ids) — fetch the full coupon for the form
    try {
      const full = await svc.getCoupon(coupon.id);
      setModalInitial(full);
      setModalOpen(true);
    } catch (err) {
      setError(err.message || 'Could not load coupon.');
    }
  };

  const handleSubmit = async (payload) => {
    if (modalMode === 'edit' && modalInitial) {
      await svc.updateCoupon(modalInitial.id, payload);
    } else {
      await svc.createCoupon(payload);
    }
    await load();
  };

  const handleAskDelete = (coupon) => {
    setPendingDelete(coupon);
    setDeleteError(null);
    setConfirmOpen(true);
  };

  const closeConfirm = () => {
    setConfirmOpen(false);
    setPendingDelete(null);
    setDeleteError(null);
  };

  const handleConfirmDelete = async () => {
    if (!pendingDelete) return;
    try {
      await svc.deleteCoupon(pendingDelete.id);
      closeConfirm();
      await load();
    } catch (err) {
      setDeleteError(err.message || 'Delete failed');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Coupons</h1>
          <p className="text-sm text-black/55 mt-1 max-w-md">Checkout discount codes — percent or fixed, scoped to the whole order, categories, or products.</p>
        </div>
        <button
          onClick={handleCreate}
          className="bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase px-4 py-3 hover:bg-[#E83354] transition-colors"
        >
          + New coupon
        </button>
      </div>

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs">{error}</div>
      )}

      <div className="bg-white border border-black/10 overflow-hidden">
        <div className="hidden md:grid grid-cols-[1.2fr_0.8fr_1fr_1.2fr_0.7fr_0.7fr_0.9fr] gap-3 px-4 py-3 text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 border-b border-black/10 bg-black/[0.02]">
          <span>Code</span>
          <span>Value</span>
          <span>Scope</span>
          <span>Window</span>
          <span>Used</span>
          <span>Status</span>
          <span className="text-right">Actions</span>
        </div>

        {loading ? (
          <div className="px-6 py-10 text-center text-sm text-black/40">Loading...</div>
        ) : coupons.length === 0 ? (
          <div className="px-6 py-16 text-center text-sm text-black/50">No coupons yet. Create your first one.</div>
        ) : (
          <ul className="divide-y divide-black/5">
            {coupons.map((c) => (
              <CouponRow key={c.id} coupon={c} onEdit={() => handleEdit(c)} onDelete={() => handleAskDelete(c)} />
            ))}
          </ul>
        )}
      </div>

      <CouponFormModal
        open={modalOpen}
        mode={modalMode}
        initial={modalInitial}
        categories={categories}
        onClose={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />

      <ConfirmDialog
        open={confirmOpen}
        title={deleteError ? 'Cannot delete' : 'Delete coupon?'}
        message={
          deleteError
            ? deleteError
            : `"${pendingDelete?.code}" will be permanently removed. Coupons already used on orders cannot be deleted — disable them instead.`
        }
        confirmLabel={deleteError ? 'Got it' : 'Delete'}
        cancelLabel="Cancel"
        tone={deleteError ? 'default' : 'danger'}
        hideCancel={!!deleteError}
        onCancel={closeConfirm}
        onConfirm={deleteError ? closeConfirm : handleConfirmDelete}
      />
    </div>
  );
}

function CouponRow({ coupon, onEdit, onDelete }) {
  return (
    <li className="grid grid-cols-2 md:grid-cols-[1.2fr_0.8fr_1fr_1.2fr_0.7fr_0.7fr_0.9fr] gap-3 px-4 py-3 items-center text-sm">
      <div className="font-mono font-bold truncate">{coupon.code}</div>
      <div className="hidden md:block text-xs">{formatValue(coupon)}</div>
      <div className="hidden md:block text-xs text-black/60">{SCOPE_LABEL[coupon.scope] ?? coupon.scope}</div>
      <div className="hidden md:block text-[11px] text-black/50">{formatWindow(coupon)}</div>
      <div className="hidden md:block text-xs">
        {coupon.usedCount ?? 0}{coupon.maxUses != null ? ` / ${coupon.maxUses}` : ''}
      </div>
      <div className="hidden md:block">
        <StatusBadge status={coupon.status} />
      </div>
      <div className="flex justify-end gap-2">
        <button
          onClick={onEdit}
          className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black/15 px-2 py-1 hover:border-black"
        >
          Edit
        </button>
        <button
          onClick={onDelete}
          className="text-[10px] font-bold tracking-[0.15em] uppercase border border-[#E83354]/30 text-[#E83354] px-2 py-1 hover:bg-[#E83354] hover:text-white hover:border-[#E83354]"
        >
          Delete
        </button>
      </div>
    </li>
  );
}

function StatusBadge({ status }) {
  const active = status === 'ACTIVE';
  return (
    <span className={`text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 ${active ? 'bg-emerald-600 text-white' : 'bg-black/40 text-white'}`}>
      {active ? 'Active' : 'Disabled'}
    </span>
  );
}
