import { useCallback, useEffect, useState } from 'react';
import ConfirmDialog from '../../components/ConfirmDialog';
import BrandFormModal from '../../components/admin/BrandFormModal';
import * as svc from '../../services/adminBrandService';

export default function AdminBrandsPage() {
  const [brands, setBrands] = useState([]);
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
      const data = await svc.listBrands();
      setBrands(data ?? []);
    } catch (err) {
      setError(err.message || 'Could not load brands.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = () => {
    setModalMode('create');
    setModalInitial(null);
    setModalOpen(true);
  };

  const handleEdit = (brand) => {
    setModalMode('edit');
    setModalInitial(brand);
    setModalOpen(true);
  };

  const handleSubmit = async (payload) => {
    if (modalMode === 'edit' && modalInitial) {
      await svc.updateBrand(modalInitial.id, payload);
    } else {
      await svc.createBrand(payload);
    }
    await load();
  };

  const handleAskDelete = (brand) => {
    setPendingDelete(brand);
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
      await svc.deleteBrand(pendingDelete.id);
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
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Brands</h1>
          <p className="text-sm text-black/55 mt-1 max-w-md">Manufacturer labels surfaced across the storefront and search filters.</p>
        </div>
        <button
          onClick={handleCreate}
          className="bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase px-4 py-3 hover:bg-[#E83354] transition-colors"
        >
          + New brand
        </button>
      </div>

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs">{error}</div>
      )}

      <div className="bg-white border border-black/10 overflow-hidden">
        <div className="hidden md:grid grid-cols-[0.5fr_2fr_1fr_0.7fr_0.7fr_0.8fr] gap-3 px-4 py-3 text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 border-b border-black/10 bg-black/[0.02]">
          <span>Logo</span>
          <span>Name</span>
          <span>Slug</span>
          <span>Products</span>
          <span>Status</span>
          <span className="text-right">Actions</span>
        </div>

        {loading ? (
          <div className="px-6 py-10 text-center text-sm text-black/40">Loading...</div>
        ) : brands.length === 0 ? (
          <div className="px-6 py-16 text-center text-sm text-black/50">No brands yet. Create your first one.</div>
        ) : (
          <ul className="divide-y divide-black/5">
            {brands.map((b) => (
              <BrandRow
                key={b.id}
                brand={b}
                onEdit={() => handleEdit(b)}
                onDelete={() => handleAskDelete(b)}
              />
            ))}
          </ul>
        )}
      </div>

      <BrandFormModal
        open={modalOpen}
        mode={modalMode}
        initial={modalInitial}
        onClose={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />

      <ConfirmDialog
        open={confirmOpen}
        title={deleteError ? 'Cannot delete' : 'Delete brand?'}
        message={
          deleteError
            ? deleteError
            : `"${pendingDelete?.name}" will be permanently removed. Brands that still have products cannot be deleted.`
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

function BrandRow({ brand, onEdit, onDelete }) {
  return (
    <li className="grid grid-cols-2 md:grid-cols-[0.5fr_2fr_1fr_0.7fr_0.7fr_0.8fr] gap-3 px-4 py-3 items-center text-sm">
      <div className="hidden md:block">
        {brand.logoUrl ? (
          <img src={brand.logoUrl} alt={brand.name} className="w-10 h-10 object-contain border border-black/10" />
        ) : (
          <div className="w-10 h-10 bg-black/5 border border-black/10 flex items-center justify-center text-[9px] text-black/40 uppercase">
            {brand.name?.slice(0, 2)}
          </div>
        )}
      </div>
      <div className="min-w-0">
        <p className="font-bold truncate">{brand.name}</p>
        {brand.websiteUrl && <p className="text-[11px] text-black/40 truncate">{brand.websiteUrl}</p>}
      </div>
      <div className="hidden md:block text-xs text-black/50 font-mono truncate">{brand.slug}</div>
      <div className="hidden md:block text-xs">{brand.productCount ?? 0}</div>
      <div className="hidden md:block">
        <StatusBadge active={brand.isActive} />
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

function StatusBadge({ active }) {
  if (active) {
    return (
      <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-emerald-600 text-white">
        Active
      </span>
    );
  }
  return (
    <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-[#E83354] text-white">
      Hidden
    </span>
  );
}
