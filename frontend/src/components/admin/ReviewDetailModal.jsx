import StarRating from '../StarRating';
import ReviewStatusBadge from './ReviewStatusBadge';

export default function ReviewDetailModal({ review, onClose, onApprove, onReject, onDelete }) {
  if (!review) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4" onClick={onClose}>
      <div
        className="bg-white max-w-lg w-full max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between p-5 border-b border-black/10">
          <h3 className="font-['Anton'] text-2xl uppercase tracking-tight">Review</h3>
          <button onClick={onClose} className="text-black/50 hover:text-black text-2xl leading-none" aria-label="Close">×</button>
        </div>

        <div className="p-5 space-y-4">
          <div className="flex items-center gap-3 flex-wrap">
            <StarRating value={review.rating} size={16} />
            <ReviewStatusBadge status={review.status} />
            {review.verifiedPurchase && (
              <span className="text-[9px] font-bold tracking-wider uppercase text-green-700">Verified</span>
            )}
            <span className="text-[11px] text-black/40 ml-auto">{formatDate(review.createdAt)}</span>
          </div>

          <Field label="Product">
            {review.productId ? (
              <a
                href={`/product/${review.productId}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm font-bold hover:text-[#E83354] transition-colors"
              >
                {review.productName}
              </a>
            ) : (
              <p className="text-sm font-bold">{review.productName}</p>
            )}
          </Field>

          <Field label="Customer">
            <p className="text-sm">{review.userName}</p>
            <p className="text-xs text-black/50">{review.userEmail}</p>
          </Field>

          {review.title && (
            <Field label="Title"><p className="text-sm font-bold">{review.title}</p></Field>
          )}

          <Field label="Review">
            {review.body
              ? <p className="text-sm text-black/80 whitespace-pre-wrap break-words">{review.body}</p>
              : <p className="text-sm text-black/30 italic">No written review.</p>}
          </Field>

          {review.images?.length > 0 && (
            <Field label={`Photos (${review.images.length})`}>
              <div className="flex flex-wrap gap-2">
                {review.images.map((src, i) => (
                  <a key={i} href={src} target="_blank" rel="noopener noreferrer">
                    <img src={src} alt={`review photo ${i + 1}`} className="w-20 h-20 object-cover border border-black/10" />
                  </a>
                ))}
              </div>
            </Field>
          )}

          <p className="text-xs text-black/50">{review.helpfulCount} found this helpful</p>
        </div>

        <div className="flex justify-end gap-2 p-5 border-t border-black/10">
          {review.status !== 'APPROVED' && (
            <ActionButton onClick={onApprove} variant="success">Approve</ActionButton>
          )}
          {review.status !== 'REJECTED' && (
            <ActionButton onClick={onReject} variant="warning">Reject</ActionButton>
          )}
          <ActionButton onClick={onDelete} variant="danger">Delete</ActionButton>
        </div>
      </div>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <div>
      <p className="text-[10px] font-bold tracking-[0.15em] uppercase text-black/40 mb-1">{label}</p>
      {children}
    </div>
  );
}

function ActionButton({ onClick, variant, children }) {
  const palette = {
    success: 'border-emerald-600/40 text-emerald-700 hover:bg-emerald-600 hover:text-white hover:border-emerald-600',
    warning: 'border-amber-600/40 text-amber-700 hover:bg-amber-600 hover:text-white hover:border-amber-600',
    danger: 'border-[#E83354]/30 text-[#E83354] hover:bg-[#E83354] hover:text-white hover:border-[#E83354]',
  }[variant];
  return (
    <button
      type="button"
      onClick={onClick}
      className={`text-[11px] font-bold tracking-[0.15em] uppercase border px-4 py-2 transition-colors ${palette}`}
    >
      {children}
    </button>
  );
}

function formatDate(iso) {
  if (!iso) return '-';
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}
