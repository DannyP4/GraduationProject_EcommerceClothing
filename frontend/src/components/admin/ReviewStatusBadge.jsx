const STYLES = {
  PENDING:  'bg-amber-50     text-amber-700  border-amber-300',
  APPROVED: 'bg-green-50     text-green-700  border-green-400',
  REJECTED: 'bg-[#E83354]/10 text-[#E83354] border-[#E83354]/50',
};

export default function ReviewStatusBadge({ status }) {
  const cls = STYLES[status] ?? 'bg-black/5 text-black/60 border-black/15';
  return (
    <span className={`font-bold tracking-wider uppercase border text-[10px] px-2 py-0.5 ${cls}`}>
      {status}
    </span>
  );
}
