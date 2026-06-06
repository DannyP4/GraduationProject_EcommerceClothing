import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import ConfirmDialog from '../../components/ConfirmDialog';
import StarRating from '../../components/StarRating';
import ReviewStatusBadge from '../../components/admin/ReviewStatusBadge';
import ReviewDetailModal from '../../components/admin/ReviewDetailModal';
import AdminPagination from '../../components/admin/AdminPagination';
import { useToast } from '../../components/Toast';
import useScrollRestore from '../../lib/useScrollRestore';
import * as reviewSvc from '../../services/adminReviewService';

const STATUS_OPTIONS = [
  { value: '', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
];
const SEARCH_DEBOUNCE_MS = 400;

export default function AdminReviewsPage() {
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();

  const [page, setPage] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [searchInput, setSearchInput] = useState(() => searchParams.get('q') ?? '');
  const [search, setSearch] = useState(() => searchParams.get('q') ?? '');
  const [status, setStatus] = useState(() => searchParams.get('status') ?? '');
  const [pageIndex, setPageIndex] = useState(() => Math.max(0, Math.floor(Number(searchParams.get('page')) || 1) - 1));
  const pageSize = 20;

  const [confirmReject, setConfirmReject] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [detail, setDetail] = useState(null);

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
    if (status) next.set('status', status);
    if (search) next.set('q', search);
    const review = searchParams.get('review');
    if (review) next.set('review', review);
    if (next.toString() !== searchParams.toString()) {
      setSearchParams(next, { replace: true });
    }
  }, [pageIndex, status, search, searchParams, setSearchParams]);

  const filtersDirty = !!(searchInput || status);

  const filterParams = useMemo(() => ({
    page: pageIndex,
    size: pageSize,
    search: search || undefined,
    status: status || undefined,
  }), [search, status, pageIndex]);

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    setError(null);
    try {
      const data = await reviewSvc.listReviews(filterParams);
      setPage(data);
    } catch (err) {
      setError(err.message || 'Could not load reviews.');
    } finally {
      if (!silent) setLoading(false);
    }
  }, [filterParams]);

  useEffect(() => { load(); }, [load]);

  const clearFilters = () => {
    setSearchInput('');
    setSearch('');
    setStatus('');
    setPageIndex(0);
  };

  const refresh = async () => {
    setRefreshing(true);
    try { await load(true); } finally { setRefreshing(false); }
  };

  const handleApprove = async (review) => {
    try {
      await reviewSvc.approveReview(review.id);
      toast.success('Review approved');
      await load(true);
    } catch (err) {
      toast.error(err.message || 'Approve failed');
    }
  };

  const handleReject = async () => {
    if (!confirmReject) return;
    try {
      await reviewSvc.rejectReview(confirmReject.id);
      setConfirmReject(null);
      toast.success('Review rejected — hidden from storefront');
      await load(true);
    } catch (err) {
      toast.error(err.message || 'Reject failed');
    }
  };

  const handleDelete = async () => {
    if (!confirmDelete) return;
    try {
      await reviewSvc.deleteReview(confirmDelete.id);
      setConfirmDelete(null);
      toast.success('Review deleted');
      await load(true);
    } catch (err) {
      toast.error(err.message || 'Delete failed');
    }
  };

  const reviewParam = searchParams.get('review');
  useEffect(() => {
    if (!reviewParam) return;
    let cancelled = false;
    reviewSvc.getReview(reviewParam).then((r) => { if (!cancelled) setDetail(r); }).catch(() => {});
    return () => { cancelled = true; };
  }, [reviewParam]);

  const closeDetail = () => {
    setDetail(null);
    if (searchParams.get('review')) {
      const next = new URLSearchParams(searchParams);
      next.delete('review');
      setSearchParams(next, { replace: true });
    }
  };

  const detailApprove = async () => {
    if (!detail) return;
    try {
      await reviewSvc.approveReview(detail.id);
      toast.success('Review approved');
      await load(true);
      closeDetail();
    } catch (err) {
      toast.error(err.message || 'Approve failed');
    }
  };
  const detailReject = () => { const r = detail; closeDetail(); setConfirmReject(r); };
  const detailDelete = () => { const r = detail; closeDetail(); setConfirmDelete(r); };

  const content = page?.content ?? [];
  const totalPages = page?.totalPages ?? 0;
  const totalElements = page?.totalElements ?? 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-['Anton'] text-4xl md:text-5xl uppercase tracking-tight">Reviews</h1>
        <p className="text-sm text-black/55 mt-1 max-w-xl">
          Reviews auto-publish once a verified buyer submits them. Use this queue to reject (hide) anything
          that breaks community standards, or delete it outright.
        </p>
      </div>

      <div className="bg-white border border-black/10 p-3">
        <div className="flex flex-wrap items-stretch gap-2">
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search product, customer email, or review text..."
            className="flex-1 min-w-[200px] border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
          />
          <select
            value={status}
            onChange={(e) => { setStatus(e.target.value); setPageIndex(0); }}
            className="border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none bg-white min-w-[130px]"
          >
            {STATUS_OPTIONS.map((s) => <option key={s.value || 'all'} value={s.value}>{s.label}</option>)}
          </select>
          <button
            type="button"
            onClick={clearFilters}
            disabled={!filtersDirty}
            className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-4 py-2 hover:border-black hover:bg-black hover:text-white transition-colors disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-black/40"
          >
            Clear
          </button>
          <button
            type="button"
            onClick={refresh}
            disabled={refreshing}
            title="Reload reviews"
            className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-4 py-2 hover:border-black hover:bg-black hover:text-white transition-colors disabled:opacity-40 inline-flex items-center gap-1.5"
          >
            <RefreshIcon spinning={refreshing} /> {refreshing ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs">{error}</div>
      )}

      <div className="bg-white border border-black/10 overflow-hidden">
        <div className="hidden lg:grid grid-cols-[1.4fr_1.4fr_2.4fr_0.8fr_0.6fr_0.9fr] gap-3 px-4 py-3 text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 border-b border-black/10 bg-black/[0.02]">
          <span>Product</span>
          <span>Customer</span>
          <span>Review</span>
          <span>Status</span>
          <span>Helpful</span>
          <span className="text-right">Actions</span>
        </div>

        {loading ? (
          <div className="px-6 py-10 text-center text-sm text-black/40">Loading...</div>
        ) : content.length === 0 ? (
          <div className="px-6 py-16 text-center text-sm text-black/50">No reviews match these filters.</div>
        ) : (
          <ul className="divide-y divide-black/5">
            {content.map((r) => (
              <ReviewRow
                key={r.id}
                review={r}
                onOpen={() => setDetail(r)}
                onApprove={() => handleApprove(r)}
                onReject={() => setConfirmReject(r)}
                onDelete={() => setConfirmDelete(r)}
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
        open={!!confirmReject}
        title="Reject this review?"
        message="It will be hidden from the storefront immediately. You can approve it again later."
        confirmLabel="Reject"
        cancelLabel="Cancel"
        tone="danger"
        onCancel={() => setConfirmReject(null)}
        onConfirm={handleReject}
      />
      <ConfirmDialog
        open={!!confirmDelete}
        title="Delete this review?"
        message="This permanently removes the review, its images and helpful votes. This cannot be undone."
        confirmLabel="Delete"
        cancelLabel="Cancel"
        tone="danger"
        onCancel={() => setConfirmDelete(null)}
        onConfirm={handleDelete}
      />

      {detail && (
        <ReviewDetailModal
          review={detail}
          onClose={closeDetail}
          onApprove={detailApprove}
          onReject={detailReject}
          onDelete={detailDelete}
        />
      )}
    </div>
  );
}

function ReviewRow({ review, onOpen, onApprove, onReject, onDelete }) {
  const excerpt = review.body && review.body.length > 140 ? `${review.body.slice(0, 140)}…` : review.body;
  return (
    <li className="grid grid-cols-1 lg:grid-cols-[1.4fr_1.4fr_2.4fr_0.8fr_0.6fr_0.9fr] gap-3 px-4 py-3 items-center text-sm">
      <div className="min-w-0">
        <button onClick={onOpen} className="font-bold truncate block max-w-full text-left hover:text-[#E83354] transition-colors">
          {review.productName}
        </button>
        <p className="text-[11px] text-black/40">{formatDateShort(review.createdAt)}</p>
      </div>
      <div className="min-w-0">
        <p className="text-xs text-black/70 truncate">{review.userEmail}</p>
        <p className="text-[11px] text-black/40 truncate">{review.userName}</p>
      </div>
      <div className="min-w-0">
        <div className="flex items-center gap-2 mb-1 flex-wrap">
          <StarRating value={review.rating} size={13} />
          {review.verifiedPurchase && (
            <span className="text-[9px] font-bold tracking-wider uppercase text-green-700">Verified</span>
          )}
          {review.images?.length > 0 && (
            <span className="text-[10px] text-black/40">{review.images.length} photo{review.images.length > 1 ? 's' : ''}</span>
          )}
        </div>
        {review.title && <p className="text-xs font-bold">{review.title}</p>}
        {review.body
          ? <button onClick={onOpen} className="text-xs text-black/60 break-words text-left hover:text-black transition-colors">{excerpt}</button>
          : <p className="text-xs text-black/30 italic">No written review.</p>}
      </div>
      <div className="lg:pt-0.5">
        <ReviewStatusBadge status={review.status} />
      </div>
      <div className="text-xs text-black/60 lg:pt-0.5">{review.helpfulCount}</div>
      <div className="flex lg:justify-end gap-1.5">
        {review.status !== 'APPROVED' && (
          <IconButton title="Approve" onClick={onApprove} variant="success"><CheckIcon /></IconButton>
        )}
        {review.status !== 'REJECTED' && (
          <IconButton title="Reject (hide)" onClick={onReject} variant="warning"><EyeOffIcon /></IconButton>
        )}
        <IconButton title="Delete permanently" onClick={onDelete} variant="danger"><TrashIcon /></IconButton>
      </div>
    </li>
  );
}

function IconButton({ title, onClick, variant, children }) {
  const palette = {
    success: 'border-emerald-600/40 text-emerald-700 hover:bg-emerald-600 hover:text-white hover:border-emerald-600',
    warning: 'border-amber-600/40 text-amber-700 hover:bg-amber-600 hover:text-white hover:border-amber-600',
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

function CheckIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}
function EyeOffIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
      <line x1="1" y1="1" x2="23" y2="23" />
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

function RefreshIcon({ spinning }) {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={spinning ? 'animate-spin' : ''}>
      <polyline points="23 4 23 10 17 10" />
      <polyline points="1 20 1 14 7 14" />
      <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
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
