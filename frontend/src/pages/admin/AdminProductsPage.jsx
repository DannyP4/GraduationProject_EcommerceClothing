import { useCallback, useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import ConfirmDialog from '../../components/ConfirmDialog';
import { useToast } from '../../components/Toast';
import * as productSvc from '../../services/adminProductService';
import * as brandSvc from '../../services/adminBrandService';
import * as catSvc from '../../services/adminCategoryService';
import useScrollRestore from '../../lib/useScrollRestore';
import AdminPagination from '../../components/admin/AdminPagination';

const GENDER_OPTIONS = ['MEN', 'WOMEN', 'UNISEX', 'KIDS'];
const STATUS_OPTIONS = [
  { value: 'active', label: 'Active' },
  { value: 'inactive', label: 'Inactive' },
  { value: 'deleted', label: 'Deleted' },
  { value: 'all', label: 'All' },
];
const SEARCH_DEBOUNCE_MS = 400;

function formatPrice(value) {
  if (value == null) return '';
  const num = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(num)) return String(value);
  return new Intl.NumberFormat('vi-VN').format(num) + ' ₫';
}

export default function AdminProductsPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const [page, setPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);

  const [searchParams, setSearchParams] = useSearchParams();
  const [searchInput, setSearchInput] = useState(() => searchParams.get('q') ?? '');
  const [search, setSearch] = useState(() => searchParams.get('q') ?? '');
  const [brandId, setBrandId] = useState(() => searchParams.get('brand') ?? '');
  const [categoryId, setCategoryId] = useState(() => searchParams.get('category') ?? '');
  const [gender, setGender] = useState(() => searchParams.get('gender') ?? '');
  const [status, setStatus] = useState(() => searchParams.get('status') ?? 'active');
  const [pageIndex, setPageIndex] = useState(() => Math.max(0, Math.floor(Number(searchParams.get('page')) || 1) - 1));
  const pageSize = 20;

  const [confirmDelete, setConfirmDelete] = useState(null);
  const [confirmRestore, setConfirmRestore] = useState(null);
  const [hardDeleteStage, setHardDeleteStage] = useState(null);

  useScrollRestore(!loading);

  useEffect(() => {
    const id = setTimeout(() => {
      const trimmed = searchInput.trim();
      if (trimmed !== search) {
        setSearch(trimmed);
        setPageIndex(0);
      }
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(id);
  }, [searchInput, search]);

  useEffect(() => {
    const next = new URLSearchParams();
    if (pageIndex > 0) next.set('page', String(pageIndex + 1));
    if (search) next.set('q', search);
    if (brandId) next.set('brand', brandId);
    if (categoryId) next.set('category', categoryId);
    if (gender) next.set('gender', gender);
    if (status !== 'active') next.set('status', status);
    if (next.toString() !== searchParams.toString()) {
      setSearchParams(next, { replace: true });
    }
  }, [pageIndex, search, brandId, categoryId, gender, status, searchParams, setSearchParams]);

  const filtersDirty = !!(searchInput || brandId || categoryId || gender || status !== 'active');

  const filterParams = useMemo(() => {
    const params = { page: pageIndex, size: pageSize, sort: 'updatedAt,desc' };
    if (search) params.search = search;
    if (brandId) params.brandId = brandId;
    if (categoryId) params.categoryId = categoryId;
    if (gender) params.gender = gender;
    if (status === 'active') { params.isActive = true; params.deleted = 'none'; }
    else if (status === 'inactive') { params.isActive = false; params.deleted = 'none'; }
    else if (status === 'deleted') { params.deleted = 'only'; }
    else if (status === 'all') { params.deleted = 'both'; }
    return params;
  }, [search, brandId, categoryId, gender, status, pageIndex]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await productSvc.listProducts(filterParams);
      setPage(data);
    } catch (err) {
      setError(err.message || 'Could not load products.');
    } finally {
      setLoading(false);
    }
  }, [filterParams]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    let cancelled = false;
    Promise.all([brandSvc.listBrands(), catSvc.listCategories()])
      .then(([b, c]) => { if (!cancelled) { setBrands(b ?? []); setCategories(c ?? []); } })
      .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const clearFilters = () => {
    setSearchInput('');
    setSearch('');
    setBrandId('');
    setCategoryId('');
    setGender('');
    setStatus('active');
    setPageIndex(0);
  };

  const handleDelete = async () => {
    if (!confirmDelete) return;
    try {
      await productSvc.softDeleteProduct(confirmDelete.id);
      const name = confirmDelete.name;
      setConfirmDelete(null);
      toast.success(`Soft-deleted "${name}"`);
      await load();
    } catch (err) {
      toast.error(err.message || 'Delete failed');
    }
  };

  const handleRestore = async () => {
    if (!confirmRestore) return;
    try {
      await productSvc.restoreProduct(confirmRestore.id);
      const name = confirmRestore.name;
      setConfirmRestore(null);
      toast.success(`Restored "${name}"`);
      await load();
    } catch (err) {
      toast.error(err.message || 'Restore failed');
    }
  };

  const handleHardDelete = async () => {
    if (!hardDeleteStage) return;
    try {
      await productSvc.hardDeleteProduct(hardDeleteStage.id);
      const name = hardDeleteStage.name;
      setHardDeleteStage(null);
      toast.success(`Permanently deleted "${name}"`);
      await load();
    } catch (err) {
      toast.error(err.message || 'Hard delete failed');
    }
  };

  const content = page?.content ?? [];
  const totalPages = page?.totalPages ?? 0;
  const totalElements = page?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Products</h1>
          <p className="text-sm text-black/55 mt-1 max-w-md">Catalog editor — basics, variants, and images.</p>
        </div>
        <Link
          to="/admin/products/new"
          className="bg-black text-white text-[11px] font-bold tracking-[0.15em] uppercase px-4 py-3 hover:bg-[#E83354] transition-colors"
        >
          + New product
        </Link>
      </div>

      <div className="bg-white border border-black/10 p-3">
        <div className="flex flex-wrap items-stretch gap-2">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search name or slug..."
            className="flex-1 min-w-[180px] border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
          />
          <select
            value={brandId}
            onChange={(e) => { setBrandId(e.target.value); setPageIndex(0); }}
            className="border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white min-w-[130px]"
          >
            <option value="">All brands</option>
            {brands.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
          </select>
          <select
            value={categoryId}
            onChange={(e) => { setCategoryId(e.target.value); setPageIndex(0); }}
            className="border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white min-w-[130px]"
          >
            <option value="">All categories</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select
            value={gender}
            onChange={(e) => { setGender(e.target.value); setPageIndex(0); }}
            className="border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white min-w-[110px]"
          >
            <option value="">All genders</option>
            {GENDER_OPTIONS.map((g) => <option key={g} value={g}>{g}</option>)}
          </select>
          <select
            value={status}
            onChange={(e) => { setStatus(e.target.value); setPageIndex(0); }}
            className="border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white min-w-[110px]"
          >
            {STATUS_OPTIONS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
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
        <div className="hidden md:grid grid-cols-[60px_2fr_1fr_1fr_0.8fr_0.8fr_0.6fr_1fr] gap-3 px-4 py-3 text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 border-b border-black/10 bg-black/[0.02]">
          <span>Image</span>
          <span>Name</span>
          <span>Brand</span>
          <span>Category</span>
          <span>Price</span>
          <span>Variants</span>
          <span>Status</span>
          <span className="text-right">Actions</span>
        </div>

        {loading ? (
          <div className="px-6 py-10 text-center text-sm text-black/40">Loading...</div>
        ) : content.length === 0 ? (
          <div className="px-6 py-16 text-center text-sm text-black/50">No products match these filters.</div>
        ) : (
          <ul className="divide-y divide-black/5">
            {content.map((p) => (
              <ProductRow
                key={p.id}
                product={p}
                onView={() => navigate(`/admin/products/${p.id}?view=1`, { state: { backTo: `/admin/products${location.search}` } })}
                onEdit={() => navigate(`/admin/products/${p.id}`, { state: { backTo: `/admin/products${location.search}` } })}
                onDelete={() => setConfirmDelete(p)}
                onRestore={() => setConfirmRestore(p)}
                onHardDelete={() => setHardDeleteStage(p)}
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
        open={!!confirmDelete}
        title="Soft-delete product?"
        message={`"${confirmDelete?.name}" will be hidden from the storefront. You can restore it from the Deleted tab.`}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        tone="danger"
        onCancel={() => setConfirmDelete(null)}
        onConfirm={handleDelete}
      />
      <ConfirmDialog
        open={!!confirmRestore}
        title="Restore product?"
        message={`"${confirmRestore?.name}" will be visible again but stays inactive until you toggle it on.`}
        confirmLabel="Restore"
        cancelLabel="Cancel"
        onCancel={() => setConfirmRestore(null)}
        onConfirm={handleRestore}
      />
      <HardDeleteDialog
        product={hardDeleteStage}
        onCancel={() => setHardDeleteStage(null)}
        onConfirm={handleHardDelete}
      />
    </div>
  );
}

function ProductRow({ product, onView, onEdit, onDelete, onRestore, onHardDelete }) {
  const deleted = !!product.deletedAt;
  return (
    <li className="grid grid-cols-2 md:grid-cols-[60px_2fr_1fr_1fr_0.8fr_0.8fr_0.6fr_1fr] gap-3 px-4 py-3 items-center text-sm">
      <div className="hidden md:block">
        {product.primaryImageUrl ? (
          <img src={product.primaryImageUrl} alt={product.name} className="w-12 h-12 object-cover border border-black/10" />
        ) : (
          <div className="w-12 h-12 bg-black/5 border border-black/10 flex items-center justify-center text-[9px] text-black/40 uppercase">
            N/A
          </div>
        )}
      </div>
      <div className="min-w-0">
        <button
          type="button"
          onClick={onView}
          className="font-bold truncate text-left hover:underline hover:text-[#E83354] transition-colors"
        >
          {product.name}
        </button>
        <p className="text-[10px] text-black/40 font-mono truncate">{product.slug}</p>
      </div>
      <div className="hidden md:block text-xs text-black/60 truncate">{product.brand?.name}</div>
      <div className="hidden md:block text-xs text-black/60 truncate">{product.category?.name}</div>
      <div className="hidden md:block text-xs">{formatPrice(product.basePrice)}</div>
      <div className="hidden md:block text-xs">{product.variantCount ?? 0}</div>
      <div className="hidden md:block">
        <StatusBadge product={product} />
      </div>
      <div className="flex justify-end gap-2 flex-wrap">
        <button
          type="button"
          onClick={onEdit}
          className="text-[10px] font-bold tracking-[0.15em] uppercase border border-black/15 px-2 py-1 hover:border-black"
        >
          Edit
        </button>
        {deleted ? (
          <>
            <button
              type="button"
              onClick={onRestore}
              className="text-[10px] font-bold tracking-[0.15em] uppercase border border-emerald-600/40 text-emerald-700 px-2 py-1 hover:bg-emerald-600 hover:text-white hover:border-emerald-600"
            >
              Restore
            </button>
            <button
              type="button"
              onClick={onHardDelete}
              title="Permanently delete: product, variants, and Cloudinary images"
              className="text-[10px] font-bold tracking-[0.15em] uppercase bg-[#E83354] text-white px-2 py-1 hover:bg-[#c82244]"
            >
              Delete forever
            </button>
          </>
        ) : (
          <button
            type="button"
            onClick={onDelete}
            className="text-[10px] font-bold tracking-[0.15em] uppercase border border-[#E83354]/30 text-[#E83354] px-2 py-1 hover:bg-[#E83354] hover:text-white hover:border-[#E83354]"
          >
            Delete
          </button>
        )}
      </div>
    </li>
  );
}

function HardDeleteDialog({ product, onCancel, onConfirm }) {
  const [typed, setTyped] = useState('');
  useEffect(() => { setTyped(''); }, [product]);
  if (!product) return null;
  const matches = typed === product.slug;
  return createPortal(
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 px-4"
      role="dialog"
      aria-modal="true"
      onClick={onCancel}
    >
      <div className="bg-white max-w-md w-full p-6" onClick={(e) => e.stopPropagation()}>
        <h3 className="font-['Anton'] text-2xl uppercase tracking-tight mb-2 text-[#E83354]">Delete forever?</h3>
        <p className="text-sm text-black/70 mb-2">
          This will permanently remove <strong>"{product.name}"</strong>, all its variants, and its Cloudinary images. This cannot be undone.
        </p>
        <p className="text-xs text-black/50 mb-4">
          Variants referenced by historical orders will block this operation — restore the product instead in that case.
        </p>
        <label className="block mb-5">
          <span className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/60">
            Type the slug to confirm: <span className="font-mono text-black">{product.slug}</span>
          </span>
          <input
            type="text"
            value={typed}
            onChange={(e) => setTyped(e.target.value)}
            placeholder={product.slug}
            autoFocus
            className="mt-1 w-full border border-black/20 px-3 py-2 text-sm font-mono focus:border-[#E83354] focus:outline-none"
          />
        </label>
        <div className="flex gap-3">
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 border border-black/15 text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:border-black/40"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={!matches}
            className="flex-1 bg-[#E83354] text-white text-[11px] font-bold tracking-[0.15em] uppercase py-3 hover:bg-[#c82244] disabled:opacity-30 disabled:cursor-not-allowed"
          >
            Delete forever
          </button>
        </div>
      </div>
    </div>,
    document.body,
  );
}

function StatusBadge({ product }) {
  if (product.deletedAt) {
    return <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-black text-white">Deleted</span>;
  }
  if (!product.isActive) {
    return <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-[#E83354] text-white">Hidden</span>;
  }
  return <span className="text-[10px] font-bold tracking-[0.15em] uppercase px-2 py-0.5 bg-emerald-600 text-white">Active</span>;
}
