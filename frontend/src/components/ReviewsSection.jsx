import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import StarRating from './StarRating';
import ConfirmDialog from './ConfirmDialog';
import { useToast } from './Toast';
import { useAuth } from '../context/AuthContext';
import {
  getProductReviews,
  getReviewEligibility,
  createReview,
  updateReview,
  deleteReview,
  setReviewHelpful,
  uploadReviewImage,
} from '../services/reviewService';

const PAGE_SIZE = 5;
const MAX_IMAGES = 4;

export default function ReviewsSection({ product, onChanged }) {
  const productId = product.id;
  const { status: authStatus } = useAuth();
  const isLoggedIn = authStatus === 'authenticated';
  const navigate = useNavigate();
  const toast = useToast();
  const { t } = useTranslation();

  const [reviews, setReviews] = useState([]);
  const [meta, setMeta] = useState({ totalElements: 0, totalPages: 0, hasNext: false, page: 0 });
  const [sort, setSort] = useState('newest');
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState(null);

  const [eligibility, setEligibility] = useState(null);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(null);

  const loadFirst = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getProductReviews(productId, { page: 0, size: PAGE_SIZE, sort });
      setReviews(data.content ?? []);
      setMeta({ totalElements: data.totalElements, totalPages: data.totalPages, hasNext: data.hasNext, page: 0 });
    } catch (err) {
      setError(err.message || t('reviews.errors.load'));
    } finally {
      setLoading(false);
    }
  }, [productId, sort, t]);

  useEffect(() => { loadFirst(); }, [loadFirst]);

  const refreshEligibility = useCallback(() => {
    if (!isLoggedIn) { setEligibility(null); return; }
    getReviewEligibility(productId).then(setEligibility).catch(() => setEligibility(null));
  }, [isLoggedIn, productId]);

  useEffect(() => { refreshEligibility(); }, [refreshEligibility]);

  const loadMore = async () => {
    if (!meta.hasNext || loadingMore) return;
    setLoadingMore(true);
    try {
      const next = meta.page + 1;
      const data = await getProductReviews(productId, { page: next, size: PAGE_SIZE, sort });
      setReviews((prev) => [...prev, ...(data.content ?? [])]);
      setMeta({ totalElements: data.totalElements, totalPages: data.totalPages, hasNext: data.hasNext, page: next });
    } catch (err) {
      toast.error(err.message || t('reviews.errors.loadMore'));
    } finally {
      setLoadingMore(false);
    }
  };

  const afterMutation = async () => {
    await loadFirst();
    refreshEligibility();
    onChanged?.();
  };

  const handleSubmit = async (payload) => {
    if (editing) {
      await updateReview(editing.id, payload);
      toast.success(t('reviews.toasts.updated'));
    } else {
      await createReview({ ...payload, productId });
      toast.success(t('reviews.toasts.created'));
    }
    setFormOpen(false);
    setEditing(null);
    await afterMutation();
  };

  const handleDelete = async () => {
    if (!confirmDelete) return;
    try {
      await deleteReview(confirmDelete.id);
      setConfirmDelete(null);
      toast.success(t('reviews.toasts.deleted'));
      await afterMutation();
    } catch (err) {
      toast.error(err.message || t('reviews.errors.delete'));
    }
  };

  const handleHelpful = async (review) => {
    if (!isLoggedIn) { toast.info(t('reviews.toasts.loginToVote')); return; }
    try {
      const res = await setReviewHelpful(review.id, !review.helpfulByMe);
      setReviews((prev) => prev.map((r) =>
        r.id === review.id ? { ...r, helpfulCount: res.helpfulCount, helpfulByMe: res.voted } : r));
    } catch (err) {
      toast.error(err.message || t('reviews.errors.vote'));
    }
  };

  const openCreate = () => {
    if (!isLoggedIn) { toast.info(t('reviews.toasts.loginToWrite')); navigate('/login'); return; }
    setEditing(null);
    setFormOpen(true);
  };
  const openEdit = (review) => { setEditing(review); setFormOpen(true); };

  const avg = product.averageRating;
  const count = product.reviewCount ?? 0;

  return (
    <section id="reviews" className="mt-6 pt-8 border-t border-black/10">
      <div className="flex flex-wrap items-end justify-between gap-4 mb-6">
        <div>
          <p className="text-[10px] font-bold tracking-[0.25em] uppercase text-black/40 mb-1">{t('reviews.eyebrow')}</p>
          <h2 className="font-['Anton'] text-3xl md:text-4xl uppercase tracking-tight">{t('reviews.heading')}</h2>
        </div>
        {count > 0 && (
          <div className="flex items-center gap-3">
            <span className="font-['Anton'] text-4xl leading-none">{Number(avg ?? 0).toFixed(1)}</span>
            <div>
              <StarRating value={avg ?? 0} size={16} />
              <p className="text-[11px] text-black/50 mt-0.5">{t('reviews.count', { count })}</p>
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div>
          {!isLoggedIn || eligibility?.canReview ? (
            <button
              onClick={openCreate}
              className="text-[11px] font-bold tracking-[0.15em] uppercase bg-black text-white px-5 py-2.5 hover:bg-[#E83354] transition-colors"
            >
              {t('reviews.writeReview')}
            </button>
          ) : eligibility?.reason === 'ALREADY_REVIEWED' ? (
            <span className="text-xs text-black/50">{t('reviews.alreadyReviewed')}</span>
          ) : (
            <button
              disabled
              title={t('reviews.purchaseRequired')}
              className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/20 text-black/40 px-5 py-2.5 cursor-not-allowed"
            >
              {t('reviews.writeReview')}
            </button>
          )}
        </div>
        {count > 0 && (
          <select
            value={sort}
            onChange={(e) => setSort(e.target.value)}
            className="border border-black/15 px-3 py-2 text-xs focus:border-black focus:outline-none bg-white"
          >
            <option value="newest">{t('reviews.sort.newest')}</option>
            <option value="helpful">{t('reviews.sort.helpful')}</option>
          </select>
        )}
      </div>

      {formOpen && (
        <ReviewForm
          initial={editing}
          onCancel={() => { setFormOpen(false); setEditing(null); }}
          onSubmit={handleSubmit}
        />
      )}

      {error ? (
        <div className="border border-[#E83354]/30 bg-[#E83354]/5 text-[#E83354] px-4 py-3 text-xs">{error}</div>
      ) : loading ? (
        <div className="bg-white border border-black/8 px-6 py-12 text-center text-sm text-black/40">{t('reviews.loading')}</div>
      ) : reviews.length === 0 ? (
        <div className="bg-white border border-dashed border-black/15 px-6 py-12 text-center">
          <div className="inline-flex flex-col items-center gap-2">
            <StarRating value={0} size={18} />
            <p className="text-sm font-bold uppercase tracking-wider text-black/60">{t('reviews.empty.title')}</p>
            <p className="text-xs text-black/50 max-w-sm">{t('reviews.empty.subtitle')}</p>
          </div>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {reviews.map((r) => (
              <ReviewCard key={r.id} review={r} onHelpful={handleHelpful} onEdit={openEdit} onDelete={setConfirmDelete} />
            ))}
          </div>
          <div className="mt-6 text-center">
            <p className="text-[11px] text-black/40 mb-3">{t('reviews.showing', { shown: reviews.length, total: meta.totalElements })}</p>
            {meta.hasNext && (
              <button
                onClick={loadMore}
                disabled={loadingMore}
                className="text-[11px] font-bold tracking-[0.15em] uppercase border-2 border-black px-6 py-2.5 hover:bg-black hover:text-white transition-colors disabled:opacity-50"
              >
                {loadingMore ? t('reviews.loadingMore') : t('reviews.loadMore')}
              </button>
            )}
          </div>
        </>
      )}

      <ConfirmDialog
        open={!!confirmDelete}
        title={t('reviews.deleteDialog.title')}
        message={t('reviews.deleteDialog.message')}
        confirmLabel={t('reviews.deleteDialog.confirm')}
        cancelLabel={t('reviews.deleteDialog.cancel')}
        tone="danger"
        onCancel={() => setConfirmDelete(null)}
        onConfirm={handleDelete}
      />
    </section>
  );
}

function ReviewCard({ review, onHelpful, onEdit, onDelete }) {
  const { t } = useTranslation();
  return (
    <div className="bg-white border border-black/8 p-5">
      <div className="flex items-start gap-3">
        <div className="w-9 h-9 rounded-full bg-black/10 flex items-center justify-center text-xs font-bold text-black/60 flex-shrink-0">
          {(review.authorName || '?').charAt(0).toUpperCase()}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-sm font-bold">{review.authorName}</span>
            {review.verifiedPurchase && (
              <span className="text-[9px] font-bold tracking-wider uppercase bg-green-600/10 text-green-700 px-1.5 py-0.5">
                {t('reviews.verifiedPurchase')}
              </span>
            )}
            <span className="ml-auto text-[11px] text-black/40">{formatDate(review.createdAt)}</span>
          </div>
          <div className="mt-1"><StarRating value={review.rating} size={14} /></div>
          {review.variantColor && (
            <div className="mt-1.5 inline-flex items-center gap-1.5 text-[11px] text-black/45">
              {review.variantColorHex && (
                <span className="inline-block w-3 h-3 rounded-full border border-black/10" style={{ backgroundColor: review.variantColorHex }} />
              )}
              <span>{[review.variantColor, review.variantSize].filter(Boolean).join(' / ')}</span>
            </div>
          )}
          {review.title && <p className="mt-2 text-sm font-bold">{review.title}</p>}
          <p className="mt-1 text-sm text-black/70 leading-relaxed whitespace-pre-line break-words">{review.body}</p>

          {review.images?.length > 0 && (
            <div className="flex gap-2 mt-3 flex-wrap">
              {review.images.map((url, i) => (
                <a key={i} href={url} target="_blank" rel="noreferrer" className="block w-16 h-16 overflow-hidden border border-black/10">
                  <img src={url} alt={t('reviews.imageAlt')} className="w-full h-full object-cover" />
                </a>
              ))}
            </div>
          )}

          <div className="flex items-center gap-2 mt-3">
            {review.mine ? (
              <>
                <button
                  onClick={() => onEdit(review)}
                  title={t('reviews.editReview')}
                  aria-label={t('reviews.editReview')}
                  className="w-7 h-7 inline-flex items-center justify-center border border-black/15 text-black/50 hover:border-black hover:text-black transition-colors"
                >
                  <PencilIcon />
                </button>
                <button
                  onClick={() => onDelete(review)}
                  title={t('reviews.deleteReview')}
                  aria-label={t('reviews.deleteReview')}
                  className="w-7 h-7 inline-flex items-center justify-center border border-[#E83354]/30 text-[#E83354] hover:bg-[#E83354] hover:text-white transition-colors"
                >
                  <TrashMiniIcon />
                </button>
                {review.helpfulCount > 0 && (
                  <span className="text-[11px] text-black/30 ml-1">{t('reviews.foundHelpful', { count: review.helpfulCount })}</span>
                )}
              </>
            ) : (
              <button
                onClick={() => onHelpful(review)}
                title={t('reviews.markHelpful')}
                aria-label={t('reviews.markHelpful')}
                className={`inline-flex items-center gap-1.5 text-[12px] font-bold border px-2.5 py-1 transition-colors ${
                  review.helpfulByMe
                    ? 'bg-[#E83354] text-white border-[#E83354]'
                    : 'border-black/15 text-black/50 hover:border-[#E83354] hover:text-[#E83354]'
                }`}
              >
                <ThumbsUpIcon />
                <span className="inline-flex items-center h-3.5 leading-none">({review.helpfulCount})</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function ReviewForm({ initial, onCancel, onSubmit }) {
  const toast = useToast();
  const { t } = useTranslation();
  const [rating, setRating] = useState(initial?.rating ?? 5);
  const [title, setTitle] = useState(initial?.title ?? '');
  const [body, setBody] = useState(initial?.body ?? '');
  const [images, setImages] = useState(() => (initial?.images ?? []).map((url) => ({ url, publicId: null })));
  const [uploading, setUploading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const onFiles = async (e) => {
    const files = Array.from(e.target.files || []);
    e.target.value = '';
    if (!files.length) return;
    const room = MAX_IMAGES - images.length;
    if (room <= 0) { toast.error(t('reviews.form.maxImages', { max: MAX_IMAGES })); return; }
    setUploading(true);
    try {
      for (const f of files.slice(0, room)) {
        const img = await uploadReviewImage(f);
        setImages((prev) => [...prev, img]);
      }
    } catch (err) {
      toast.error(err.message || t('reviews.form.uploadFailed'));
    } finally {
      setUploading(false);
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!body.trim()) { toast.error(t('reviews.form.bodyRequired')); return; }
    setSubmitting(true);
    try {
      await onSubmit({
        rating,
        title: title.trim() || null,
        body: body.trim(),
        images: images.map((i) => ({ url: i.url, publicId: i.publicId })),
      });
    } catch (err) {
      toast.error(err.message || t('reviews.form.submitFailed'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={submit} className="bg-white border border-black/15 p-5 mb-6 space-y-4">
      <div>
        <label className="block text-[11px] font-bold tracking-[0.15em] uppercase text-black/50 mb-2">{t('reviews.form.ratingLabel')}</label>
        <StarRating value={rating} size={28} interactive onChange={setRating} />
      </div>
      <div>
        <label className="block text-[11px] font-bold tracking-[0.15em] uppercase text-black/50 mb-2">{t('reviews.form.titleLabel')}</label>
        <input
          type="text"
          value={title}
          maxLength={255}
          onChange={(e) => setTitle(e.target.value)}
          placeholder={t('reviews.form.titlePlaceholder')}
          className="w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none"
        />
      </div>
      <div>
        <label className="block text-[11px] font-bold tracking-[0.15em] uppercase text-black/50 mb-2">{t('reviews.form.bodyLabel')}</label>
        <textarea
          value={body}
          rows={4}
          maxLength={5000}
          onChange={(e) => setBody(e.target.value)}
          placeholder={t('reviews.form.bodyPlaceholder')}
          className="w-full border border-black/15 px-3 py-2 text-sm focus:border-black focus:outline-none resize-y"
        />
      </div>
      <div>
        <label className="block text-[11px] font-bold tracking-[0.15em] uppercase text-black/50 mb-2">
          {t('reviews.form.photosLabel', { max: MAX_IMAGES })}
        </label>
        <div className="flex gap-2 flex-wrap items-center">
          {images.map((img, i) => (
            <div key={i} className="relative w-16 h-16 border border-black/10 overflow-hidden group">
              <img src={img.url} alt={t('reviews.form.uploadAlt')} className="w-full h-full object-cover" />
              <button
                type="button"
                onClick={() => setImages((prev) => prev.filter((_, idx) => idx !== i))}
                className="absolute top-0 right-0 bg-black/70 text-white w-5 h-5 text-xs leading-none flex items-center justify-center"
                aria-label={t('reviews.form.removeImage')}
              >
                ×
              </button>
            </div>
          ))}
          {images.length < MAX_IMAGES && (
            <label className="w-16 h-16 border border-dashed border-black/25 flex items-center justify-center cursor-pointer text-black/40 hover:border-black hover:text-black transition-colors text-xl">
              {uploading ? <span className="text-[10px]">…</span> : '+'}
              <input type="file" accept="image/*" multiple className="hidden" onChange={onFiles} disabled={uploading} />
            </label>
          )}
        </div>
      </div>
      <div className="flex gap-3 pt-1">
        <button
          type="submit"
          disabled={submitting || uploading}
          className="text-[11px] font-bold tracking-[0.15em] uppercase bg-black text-white px-6 py-3 hover:bg-[#E83354] transition-colors disabled:opacity-50"
        >
          {submitting ? t('reviews.form.submitting') : initial ? t('reviews.form.update') : t('reviews.form.submit')}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="text-[11px] font-bold tracking-[0.15em] uppercase border border-black/15 px-6 py-3 hover:border-black transition-colors"
        >
          {t('reviews.form.cancel')}
        </button>
      </div>
    </form>
  );
}

function ThumbsUpIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="block">
      <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" />
    </svg>
  );
}

function PencilIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
      <path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
    </svg>
  );
}

function TrashMiniIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
    </svg>
  );
}

function formatDate(iso) {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
  } catch {
    return '';
  }
}
